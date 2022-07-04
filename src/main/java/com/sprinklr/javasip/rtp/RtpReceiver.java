package com.sprinklr.javasip.rtp;

import com.sprinklr.javasip.agent.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.*;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Agent's RTP receiver which receives data packets from Ozonetel in RTP session
 */
public class RtpReceiver implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtpReceiver.class);
    private static final int RTP_BLOCK_SOCKET_TIME_MS = (int) TimeUnit.MILLISECONDS.toMillis(1000);
    private final Queue<byte[]> inboundRtpQueue;
    private final AgentConfig agentConfig;
    private volatile boolean exit = false;

    /**
     * Instantiates the RtpReceiver entity of an Agent
     * @param inboundRtpQueue The queue where the received Rtp packets are stored
     * @param agentConfig The configuration the Agent to whom this RtpReceiver entity belongs
     */
    public RtpReceiver(Queue<byte[]> inboundRtpQueue, AgentConfig agentConfig) {
        this.inboundRtpQueue = inboundRtpQueue;
        this.agentConfig = agentConfig;
    }

    /**
     * Starts listening at the specified port and address for Rtp Packets
     * @throws IOException
     */
    public void start() throws IOException {

        InetAddress localRtpIp = InetAddress.getByName(agentConfig.getRtpLocalIp());

        try (DatagramSocket serverSocket = new DatagramSocket(agentConfig.getRtpLocalPort(), localRtpIp)) {

            LOGGER.info("{} listening on udp:{}:{}", agentConfig.getAgentName(), agentConfig.getRtpLocalIp(), agentConfig.getRtpLocalPort());
            serverSocket.setSoTimeout(RTP_BLOCK_SOCKET_TIME_MS); //block on receive for specified time
            while (!exit) {
                readBytes(serverSocket);
            }
        }
        LOGGER.info("{} stopped listening on udp:{}:{}", agentConfig.getAgentName(), agentConfig.getRtpLocalIp(), agentConfig.getRtpLocalPort());
    }

    /**
     * Helper function which receives the incoming packets and pushes them into the inboound queue
     * @param serverSocket the DatagramSocket which listens for Rtp Packets
     * @throws IOException
     */
    private void readBytes(DatagramSocket serverSocket) throws IOException {
        byte[] receiveData;
        DatagramPacket receivePacket;
        try {
            receiveData = new byte[agentConfig.getRtpPacketSize()];
            receivePacket = new DatagramPacket(receiveData, agentConfig.getRtpPacketSize());
            serverSocket.receive(receivePacket);

            inboundRtpQueue.offer(receivePacket.getData());
        } catch (SocketTimeoutException e) {
            //no message received, timeout, check for exit condition in while loop
        }
    }

    /**
     * Overridden method of Runnable which starts this on a new thread
     */
    @Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            LOGGER.error("In RtpReceiver, {} alert! IOException: {}", agentConfig.getAgentName(), sw);
        }
    }

    /**
     * Stops the listener
     */
    public void stop() {
        exit = true;
    }

}
