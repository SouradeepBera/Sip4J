package com.sprinklr.javasip.rtp;

import com.sprinklr.javasip.agent.AgentConfig;
import com.sprinklr.javasip.agent.DataSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Queue;

import static com.sprinklr.javasip.utils.Constants.SLEEP_CPU_TIME_MS;

/**
Agent's RTP sender which sends data packets to Ozonetel in the RTP session
 */
public class RtpSenderImpl implements DataSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtpSenderImpl.class);

    private final Queue<byte[]> outboundRtpQueue;
    private final RtpAddress rtpRemoteAddress;
    private final AgentConfig agentConfig;

    private volatile boolean exit = false;

    /**
     * Instantiates an RtpSender object responsible for sending the processed audio data to the remote destination
     *
     * @param rtpRemoteAddress The remote RTP address where the packets are to be sent
     * @param outboundRtpQueue The queue from which data is polled and sent to the remote destination
     * @param agentConfig The configuration of the Agent to whom this RtpSender entity belongs
     */
    public RtpSenderImpl(RtpAddress rtpRemoteAddress, Queue<byte[]> outboundRtpQueue, AgentConfig agentConfig) {
        this.rtpRemoteAddress = rtpRemoteAddress;
        this.outboundRtpQueue = outboundRtpQueue;
        this.agentConfig = agentConfig;
    }

    /**
     * Starts transmission of the data packets to be sent to the remote destination in the RTP session
     */
    @Override
    public void start() {

        InetAddress remoteRtpIp = null;
        try {
            remoteRtpIp = InetAddress.getByName(rtpRemoteAddress.getAddress());
        } catch (UnknownHostException e) {
            LOGGER.error("UnknownHostException in {}: {}", agentConfig.getAgentName(), e.toString());
            return;
        }
        int remoteRtpPort = rtpRemoteAddress.getPort();

        try (DatagramSocket datagramSocket = new DatagramSocket()) {

            LOGGER.info("Starting rtp transmission from {}", agentConfig.getAgentName());

            while (!exit) {
                byte[] data = outboundRtpQueue.poll(); //packet size should be correctly configured and sent from bot websocket server side
                if (data == null) {
                    Thread.sleep(SLEEP_CPU_TIME_MS); //sleep or use blocking queue, refer https://www.baeldung.com/java-concurrent-queues
                    continue;
                }
                sendBytes(remoteRtpIp, remoteRtpPort, datagramSocket, data);
            }
        } catch (IOException e) {
            LOGGER.error("IOException in {}: {}", agentConfig.getAgentName(), e.toString());
            return;
        } catch (InterruptedException e) {
            LOGGER.error("InterruptedException in {}: {}", agentConfig.getAgentName(), e.toString());
            Thread.currentThread().interrupt();
            return;
        }
        LOGGER.info("Stopping rtp transmission from {}", agentConfig.getAgentName());
    }

    /**
     * Helper function which sends the data
     * @param remoteRtpIp the IP address of the remote RTP address
     * @param remoteRtpPort the port of the remote RTP address
     * @param datagramSocket the DatagramSocket used to send the data
     * @param data the data in bytes to be sent
     * @throws IOException
     */
    private void sendBytes(InetAddress remoteRtpIp, int remoteRtpPort, DatagramSocket datagramSocket, byte[] data) throws IOException {
        DatagramPacket sendPacket;
        sendPacket = new DatagramPacket(data, data.length, remoteRtpIp, remoteRtpPort);
        datagramSocket.send(sendPacket);
    }

    /**
     * Overridden method of Runnable which starts this on a new thread
     */
    @Override
    public void run() {
        start();
    }

    /**
     * Stops the transmission of RTP packets
     */
    public void stop() {
        exit = true;
    }

}
