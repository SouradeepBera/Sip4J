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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Queue;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import static com.sprinklr.javasip.utils.Constants.WS_RECONNECT_CODE;

/*
 * Agent class which handles signalling and media transfer. Sits between Ozonetel and Bot.
 */
public class Agent implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);
    private AgentConfig agentConfig;
    private AgentState agentState;

    public Agent(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
        this.agentState = new AgentState(agentConfig.getAgentName());
    }

    public void start() throws PeerUnavailableException, TransportNotSupportedException, TooManyListenersException, InvalidArgumentException, ObjectInUseException, ParseException, ExecutionException, InterruptedException, URISyntaxException {

        SipAllFactories sipAllFactories;
        sipAllFactories = SipAllFactories.getInstance();

        Queue<byte[]> inboundRtpQueue = new ConcurrentLinkedQueue<>();
        Queue<byte[]> outboundRtpQueue = new ConcurrentLinkedQueue<>();

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        executor.setCorePoolSize(3);
        executor.setMaximumPoolSize(6);

        SipExtension sip;
        sip = new SipExtension(sipAllFactories, agentState, agentConfig);

        /*
         * Refer to jain-sip-ri/gov.nist/javax/sip/SipStackImpl and src/main/java/com.spr/sip/Sip to understand threading
         * Currently, javax.sip.REENTRANT_LISTENER = false and defaults are used.Change properties if behaviour is to be changed
         */
        Future<RtpAddress> rtpRemoteAddressFuture = executor.submit(sip);

        RtpAddress rtpRemoteAddress;
        rtpRemoteAddress = rtpRemoteAddressFuture.get();

        if (!(rtpRemoteAddress.getAddressType().equals(agentConfig.getRtpAddressType())) || !(rtpRemoteAddress.getNetworkType().equals(agentConfig.getRtpNetworkType()))) {
            throw new IllegalStateException("Rtp address type or network type not matching");
        }

        //start listening on rtp port for rtp data from ozonetel (send data only after this is running)
        RtpReceiver rtpReceiver = new RtpReceiver(inboundRtpQueue, agentConfig);
        executor.execute(rtpReceiver); //1 new thread started

        //connect websocket to botserver (make sure botserver is running)
        Websocket websocket;
        websocket = new Websocket(outboundRtpQueue, agentState, agentConfig);
        websocket.connect(); //starts a read and write thread internally, 2 new threads started

        //send the returned data to ozontel rtp
        RtpSender rtpSender = new RtpSender(rtpRemoteAddress, outboundRtpQueue, agentConfig);
        executor.execute(rtpSender); //1 new thread started

        while (!agentState.getSipState().equals(SipState.DISCONNECTED)) {
            try {
                byte[] data = inboundRtpQueue.poll();
                if (data == null)
                    continue;
                websocket.send(data);
            } catch (WebsocketNotConnectedException e) {
                if (agentState.getWsCloseCode() == WS_RECONNECT_CODE) {
                    websocket.reconnect(); //reconnecting immediately, thread.sleep to delay
                    LOGGER.info("Reconnecting {} to bot websocket server", agentConfig.getAgentName());
                }
                else {
                    throw new WebsocketNotConnectedException();
                }
            }
        }

        rtpReceiver.stop();
        rtpSender.stop();
        websocket.close();
        executor.shutdown();
    }

    public AgentConfig getConfig() {
        return agentConfig;
    }

    public AgentState getState() {
        return agentState;
    }

    public void clear() {
        agentConfig = null;
        agentState = null;
    }

    @Override
    public void run() {
        try {
            start();
        } catch (InterruptedException e) {
            LOGGER.error("{} interrupted in Agent", agentConfig.getAgentName());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            LOGGER.error("In Agent, {} alert! \n Cause: {} \n Stacktrace: {}", agentConfig.getAgentName(), e.getCause(), sw);
        }
    }
}