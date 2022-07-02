package com.sprinklr.javasip.rtp;

import com.sprinklr.javasip.agent.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Queue;

/*
 * Agent's RTP receiver which receives data packets from Ozonetel in RTP session
 */
public class RtpReceiver implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtpReceiver.class);
    private final Queue<byte[]> inboundRtpQueue;
    private volatile boolean exit = false;
    private final AgentConfig agentConfig;

    public RtpReceiver(Queue<byte[]> inboundRtpQueue, AgentConfig agentConfig) {
        this.inboundRtpQueue = inboundRtpQueue;
        this.agentConfig = agentConfig;
    }

    public void start() {

        InetAddress localRtpIp = null;
        try {
            localRtpIp = InetAddress.getByName(agentConfig.rtpLocalIp);
        } catch (UnknownHostException e) {
            LOGGER.error("UnknownHostException in {}: {}", agentConfig.agentName, e.toString());
        }

        try (DatagramSocket serverSocket = new DatagramSocket(agentConfig.rtpLocalPort, localRtpIp)) {

            LOGGER.info("{} listening on udp:{}:{}", agentConfig.agentName, agentConfig.rtpLocalIp, agentConfig.rtpLocalPort);
            serverSocket.setSoTimeout(1000);
            while (!exit) {
                readBytes(serverSocket);
            }

        } catch (SocketException e) {
            LOGGER.error("SocketException in {}: {}", agentConfig.agentName, e.toString());
        }
        LOGGER.info("{} stopped listening on udp:{}:{}", agentConfig.agentName, agentConfig.rtpLocalIp, agentConfig.rtpLocalPort);
    }

    private void readBytes(DatagramSocket serverSocket) {
        byte[] receiveData;
        DatagramPacket receivePacket;
        try {
            receiveData = new byte[agentConfig.rtpPacketSize];
            receivePacket = new DatagramPacket(receiveData, agentConfig.rtpPacketSize);
            serverSocket.receive(receivePacket);

            inboundRtpQueue.offer(receivePacket.getData());
        } catch (SocketTimeoutException e) {
            //no message received, timeout, check for exit condition in while loop
        } catch (IOException e) {
            LOGGER.error("IOException in {}: {} ", agentConfig.agentName, e.toString());
        }
    }

    @Override
    public void run() {
        start();
    }

    public void stop() {
        exit = true;
    }

}
