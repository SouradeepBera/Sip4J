package com.sprinklr.javasip.rtp;

import com.sprinklr.javasip.agent.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.*;
import java.util.Queue;

import static com.sprinklr.javasip.utils.Constants.RTP_BLOCK_SOCKET_TIME_MS;

/*
 * Agent's RTP receiver which receives data packets from Ozonetel in RTP session
 */
public class RtpReceiver implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtpReceiver.class);
    private final Queue<byte[]> inboundRtpQueue;
    private final AgentConfig agentConfig;
    private volatile boolean exit = false;

    public RtpReceiver(Queue<byte[]> inboundRtpQueue, AgentConfig agentConfig) {
        this.inboundRtpQueue = inboundRtpQueue;
        this.agentConfig = agentConfig;
    }

    public void start() throws IOException {

        InetAddress localRtpIp = InetAddress.getByName(agentConfig.rtpLocalIp);

        try (DatagramSocket serverSocket = new DatagramSocket(agentConfig.rtpLocalPort, localRtpIp)) {

            LOGGER.info("{} listening on udp:{}:{}", agentConfig.agentName, agentConfig.rtpLocalIp, agentConfig.rtpLocalPort);
            serverSocket.setSoTimeout(RTP_BLOCK_SOCKET_TIME_MS); //block on receive for specified time
            while (!exit) {
                readBytes(serverSocket);
            }
        }
        LOGGER.info("{} stopped listening on udp:{}:{}", agentConfig.agentName, agentConfig.rtpLocalIp, agentConfig.rtpLocalPort);
    }

    private void readBytes(DatagramSocket serverSocket) throws IOException {
        byte[] receiveData;
        DatagramPacket receivePacket;
        try {
            receiveData = new byte[agentConfig.rtpPacketSize];
            receivePacket = new DatagramPacket(receiveData, agentConfig.rtpPacketSize);
            serverSocket.receive(receivePacket);

            inboundRtpQueue.offer(receivePacket.getData());
        } catch (SocketTimeoutException e) {
            //no message received, timeout, check for exit condition in while loop
        }
    }

    @Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            LOGGER.error("In RtpReceiver, {} alert! IOException: {}", agentConfig.agentName, sw);
        }
    }

    public void stop() {
        exit = true;
    }

}
