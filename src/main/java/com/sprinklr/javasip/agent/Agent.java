package com.sprinklr.javasip.agent;

import com.sprinklr.javasip.rtp.RtpAddress;
import com.sprinklr.javasip.rtp.RtpReceiver;
import com.sprinklr.javasip.rtp.RtpSender;
import com.sprinklr.javasip.sip.SipExtension;
import com.sprinklr.javasip.sip.SipAllFactories;
import com.sprinklr.javasip.sip.SipState;
import com.sprinklr.javasip.websocket.Websocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.TransportNotSupportedException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Queue;
import java.util.TooManyListenersException;
import java.util.concurrent.*;

/*
* Agent class which handles signalling and media transfer. Sits between Ozonetel and Bot.
 */
public class Agent implements Runnable{

    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);
    private AgentConfig agentConfig;
    private AgentState agentState;

    public Agent(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
        this.agentState = new AgentState(agentConfig.agentName);
    }

    public void start() {

        SipAllFactories sipAllFactories;
        try {
            sipAllFactories = SipAllFactories.getInstance();
        } catch (PeerUnavailableException e) {
            LOGGER.error("Peer Unavailable, unable to start {}", agentConfig.agentName);
            return;
        }


        Queue<byte[]> inboundRtpQueue = new ConcurrentLinkedQueue<>();
        Queue<byte[]> outboundRtpQueue = new ConcurrentLinkedQueue<>();

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        executor.setCorePoolSize(3);
        executor.setMaximumPoolSize(20);

        SipExtension sip ;
        try {
            sip = new SipExtension(sipAllFactories, agentState, agentConfig);
        } catch (PeerUnavailableException | TransportNotSupportedException | InvalidArgumentException |
                 ObjectInUseException | TooManyListenersException | ParseException e) {
            LOGGER.error("Sip object not created for {}: {}", agentConfig.agentName, e.toString());
            return;
        }
        /*
        * Refer to jain-sip-ri/gov.nist/javax/sip/SipStackImpl and src/main/java/com.spr/sip/Sip to understand threading
        * Currently, javax.sip.REENTRANT_LISTENER = false and javax.sip.THREAD_POOL_SIZE=infinity (defaults).Change properties if behaviour is to be changed
         */
        Future<RtpAddress> rtpRemoteAddressFuture = executor.submit(sip);

        RtpAddress rtpRemoteAddress;
        try {
            rtpRemoteAddress = rtpRemoteAddressFuture.get();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted future in {}: {}", agentConfig.agentName, e.toString());
            Thread.currentThread().interrupt();
            return;
        } catch (ExecutionException e) {
            LOGGER.error("Something went wrong in execution: {}", e.toString());
            return;
        }


        if(!(rtpRemoteAddress.getAddressType().equals(agentConfig.rtpAddressType)) || !(rtpRemoteAddress.getNetworkType().equals(agentConfig.rtpNetworkType)) ){
            LOGGER.error("Rtp address type or network type not matching. Check {}", agentConfig.agentName);
            return;
        }

        //start listening on rtp port for rtp data from ozonetel (send data only after this is running)
        RtpReceiver rtpReceiver = new RtpReceiver(inboundRtpQueue, agentConfig);
        executor.execute(rtpReceiver); //1 new thread started

        //connect websocket to botserver (make sure botserver is running)
        Websocket websocket;
        try {
            websocket = new Websocket(outboundRtpQueue, agentState, agentConfig);
        } catch (URISyntaxException e) {
            LOGGER.error("Websocket object not created in {}: {}", agentConfig.agentName, e.toString());
            rtpReceiver.stop();
            return;
        }
        websocket.connect(); //starts a read and write thread internally, 2 new threads started

        //send the returned data to ozontel rtp
        RtpSender rtpSender = new RtpSender(rtpRemoteAddress, outboundRtpQueue, agentConfig);
        executor.execute(rtpSender); //1 new thread started

        while(!agentState.getSipState().equals(SipState.DISCONNECTED)){
            try{
                byte[] data = inboundRtpQueue.poll();
                if(data==null)
                    continue;
                websocket.send(data);
            } catch(WebsocketNotConnectedException e){
                if(agentState.getWsCloseCode()==1006){
                    websocket.reconnect(); //reconnecting immediately, thread.sleep to delay
                    LOGGER.info("Reconnecting {} to bot websocket server", agentConfig.agentName);
                }
            }
        }

        rtpReceiver.stop();
        rtpSender.stop();
        websocket.close();
        executor.shutdown();

    }

    public AgentConfig getConfig(){
        return agentConfig;
    }

    public AgentState getState(){
        return agentState;
    }

    public void shutdown(){
        agentConfig = null;
        agentState = null;
    }

    @Override
    public void run() {
        start();
    }
}