package com.sprinklr.javasip.mockserver;

import com.sprinklr.javasip.rtp.RtpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

/*
* Mocks entity which will send data packets via RTP from Ozonetel
 */
public class RtpOzonetelSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtpOzonetelSender.class);
    private static final String IO_EXCEPTION_MSG = "IOException {}";

    /*
    Hardcoded values, will depend on Ozonetel, set here just to mimic actual flow
     */
    private static final String READ_AUDIO_FILE = "/Users/souradeep.bera/Downloads/test_audio/speech-5.wav";   //not set by us
    private static final int RTP_LOCAL_PORT = 6022; //not set by us
    private static final String RTP_LOCAL_IP = "192.168.1.8";   //not set by us
    private static final int RTP_HEADER_SIZE = 12;  //constant for everyone
    private static final int RTP_PAYLOAD_SIZE = 256; //constant for everyone
    private static final int RTP_PACKET_SIZE = RTP_HEADER_SIZE + RTP_PAYLOAD_SIZE;
    /*
    Hardcoded values end
     */

    public void run() {
        File file = new File(READ_AUDIO_FILE);

        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            LOGGER.error(IO_EXCEPTION_MSG, e.toString());
            return;
        }

        LOGGER.info("Data length {} ", fileBytes.length);

        InetAddress address;
        try {
            address = InetAddress.getByName(RTP_LOCAL_IP);
        } catch (UnknownHostException e) {
            LOGGER.error("UnknownHostException {}", e.toString());
            return;
        }

        ByteBuffer audioBuffer = ByteBuffer.wrap(fileBytes);

        int cnt = 0;
        int ssrc = (int) (System.currentTimeMillis() % (int) (1e9 + 7));
        int startTime = 0;

        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            DatagramPacket packet;
            RtpPacket rtpPacket;
            byte[] rtpBytes;
            byte[] audioChunkBytes;

            while (audioBuffer.remaining() >= RTP_PAYLOAD_SIZE) {
                cnt++;

                audioChunkBytes = new byte[RTP_PAYLOAD_SIZE];
                audioBuffer.get(audioChunkBytes, 0, RTP_PAYLOAD_SIZE);

                rtpPacket = new RtpPacket(65, cnt, startTime, ssrc, 0, audioChunkBytes, RTP_PAYLOAD_SIZE);
                rtpBytes = new byte[RTP_PACKET_SIZE];
                rtpPacket.getPacket(rtpBytes);

                packet = new DatagramPacket(rtpBytes, RTP_PACKET_SIZE, address, RTP_LOCAL_PORT);
                datagramSocket.send(packet);

                startTime += 1;
            }
            LOGGER.info("Sent {} packets from Ozonetel from {}", cnt, READ_AUDIO_FILE);
        } catch (IOException e) {
            LOGGER.error(IO_EXCEPTION_MSG, e.toString());
        }
    }

    public static void main(String[] args) {
        new RtpOzonetelSender().run();
    }
}

