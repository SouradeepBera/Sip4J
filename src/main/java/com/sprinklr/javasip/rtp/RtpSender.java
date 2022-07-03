package com.sprinklr.javasip.rtp;

import com.sprinklr.javasip.agent.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Queue;

import static com.sprinklr.javasip.utils.Constants.SLEEP_CPU_TIME_MS;

/*
Agent's RTP sender which sends data packets to Ozonetel in the RTP session
 */
public class RtpSender implements Runnable {

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

    public void start() throws InterruptedException, IOException {

        InetAddress remoteRtpIp = InetAddress.getByName(rtpRemoteAddress.getAddress());
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
        }
        LOGGER.info("Stopping rtp transmission from {}", agentConfig.getAgentName());
    }

    private void sendBytes(InetAddress remoteRtpIp, int remoteRtpPort, DatagramSocket datagramSocket, byte[] data) throws IOException {
        DatagramPacket sendPacket;
        sendPacket = new DatagramPacket(data, data.length, remoteRtpIp, remoteRtpPort);
        datagramSocket.send(sendPacket);
    }

    @Override
    public void run() {
        try {
            start();
        } catch (InterruptedException e) {
            LOGGER.error("{} interrupted in RtpSender", agentConfig.getAgentName());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            LOGGER.error("In RtpSender, {} alert! \n Cause: {} \n Stacktrace: {}", agentConfig.getAgentName(), e.getCause(), sw);
        }
    }

    public void stop() {
        exit = true;
    }

}
