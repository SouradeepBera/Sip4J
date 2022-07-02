package com.sprinklr.javasip.mockserver;

import com.sprinklr.javasip.rtp.RtpPacket;
import com.sprinklr.javasip.utils.AudioHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/*
 * Mocks entity which will receive RTP packets on Ozonetel's end
 */
public class RtpOzonetelReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtpOzonetelReceiver.class);
    private boolean exit = false;
    private final List<byte[]> storeRecv = new ArrayList<>();

    /*
    Hardcoded values, will depend on Ozonetel, set here just to mimic actual flow
     */
    private static final int RTP_HEADER_SIZE = 12;  //not set by us
    private static final int RTP_PAYLOAD_SIZE = 256; //not set by us
    private static final int RTP_PACKET_SIZE = RTP_HEADER_SIZE + RTP_PAYLOAD_SIZE;  //not set by us
    private static final int RTP_REMOTE_PORT = 6024;   //not set by us, read from invite sdp sent by Ozonetel in Sip entity
    private static final String RTP_REMOTE_IP = "192.168.1.8"; //not set by us, read from invite sdp, read from invite sdp sent by Ozonetel in Sip entity
    private static final String WRITE_AUDIO_FILE = "/Users/souradeep.bera/Downloads/test_audio/speech-5_out.wav"; //not set by us
    /*
    Hardcoded values end
     */

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        new RtpOzonetelReceiver().run(RTP_REMOTE_PORT);
    }

    public void run(int port) throws IOException, UnsupportedAudioFileException {
        try (DatagramSocket serverSocket = new DatagramSocket(port)) {
            InetAddress inetAddress = InetAddress.getByName(RTP_REMOTE_IP);

            LOGGER.info("Listening on udp:{}:{}", inetAddress, port);
            serverSocket.setSoTimeout(5000); //shutdown after 5sec, not handled by us. This is Ozonetel's side, they should shut it down accordingly

            while (!exit) {
                getBytes(serverSocket);
            }
        } catch (IOException e) {
            LOGGER.error(e.toString());
        }

        LOGGER.info("Total size={}", storeRecv.size());
        File outputFile = new File(WRITE_AUDIO_FILE);
        ByteArrayOutputStream rawBuffer = new ByteArrayOutputStream();
        for (byte[] b : storeRecv) {
            RtpPacket rtpPacket = new RtpPacket(b, RTP_PACKET_SIZE);
            byte[] payload = new byte[RTP_PAYLOAD_SIZE];
            rtpPacket.getPayload(payload);
            rawBuffer.write(payload, 0, payload.length);
        }
        rawBuffer.flush();
        rawBuffer.close();
        AudioHelper.generateFile(rawBuffer.toByteArray(), outputFile);
        LOGGER.info("Audio saved to {}", WRITE_AUDIO_FILE);
    }

    private void getBytes(DatagramSocket serverSocket) throws IOException {
        DatagramPacket receivePacket;
        byte[] receiveData;
        try {
            receiveData = new byte[RTP_PACKET_SIZE];
            receivePacket = new DatagramPacket(receiveData, RTP_PACKET_SIZE);
            serverSocket.receive(receivePacket);
            storeRecv.add(receivePacket.getData());
        } catch (SocketTimeoutException e) {
            exit = true;
        }
    }
}
