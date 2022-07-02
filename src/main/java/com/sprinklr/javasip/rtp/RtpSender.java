package com.sprinklr.javasip.rtp;

import com.sprinklr.javasip.agent.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/*
Agent's RTP sender which sends data packets to Ozonetel in the RTP session
 */
public class RtpSender implements Runnable{

    private static final Logger LOGGER = LoggerFactory.getLogger(RtpSender.class);

    private final Queue<byte[]> outboundRtpQueue;
    private final RtpAddress rtpRemoteAddress;
    private final AgentConfig agentConfig;

    private volatile boolean exit = false;


    public RtpSender(RtpAddress rtpRemoteAddress, Queue<byte[]> outboundRtpQueue, AgentConfig agentConfig) {
        this.rtpRemoteAddress = rtpRemoteAddress;
        this.outboundRtpQueue = outboundRtpQueue;
        this.agentConfig = agentConfig;
    }

    public void start() throws InterruptedException {

        InetAddress remoteRtpIp = null;
        int remoteRtpPort = rtpRemoteAddress.getPort();
        try{
            remoteRtpIp = InetAddress.getByName(rtpRemoteAddress.getAddress());
        } catch (UnknownHostException e) {
            LOGGER.error("UnknownHostException in {}: {}", agentConfig.agentName, e.toString());
        }

        try(DatagramSocket datagramSocket = new DatagramSocket()){

            LOGGER.info("Starting rtp transmission from {}", agentConfig.agentName);

            while(!exit){

                byte[] data = outboundRtpQueue.poll(); //packet size should be correctly configured and sent from bot websocket server side
                if(data == null) {
                    Thread.sleep(TimeUnit.MILLISECONDS.toMillis(20)); //sleep or use blocking queue, refer https://www.baeldung.com/java-concurrent-queues
                    continue;
                }

                sendBytes(remoteRtpIp, remoteRtpPort, datagramSocket, data);
            }

        } catch (SocketException e) {
            LOGGER.error("SocketException in {}: {}", agentConfig.agentName, e.toString());
        }

        LOGGER.info("Stopping rtp transmission from {}", agentConfig.agentName);

    }

    private void sendBytes(InetAddress remoteRtpIp, int remoteRtpPort, DatagramSocket datagramSocket, byte[] data) {
        DatagramPacket sendPacket;
        try{
            sendPacket = new DatagramPacket(data, data.length, remoteRtpIp, remoteRtpPort);
            datagramSocket.send(sendPacket);
        } catch(UnknownHostException e){
            LOGGER.error("UnknownHostException in {}: {}", agentConfig.agentName, e.toString());
        } catch(IOException e){
            LOGGER.error("IOException in {}: {} ", agentConfig.agentName, e.toString());
        }
    }

    @Override
    public void run() {
        try {
            start();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for data to send from outbound queue: {}", e.toString());
            stop();
            Thread.currentThread().interrupt();
        }
    }

    public void stop(){
        exit = true;
    }

}
