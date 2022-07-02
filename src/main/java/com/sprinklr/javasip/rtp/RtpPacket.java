package com.sprinklr.javasip.rtp;

import java.util.Arrays;

import static com.sprinklr.javasip.utils.ConstantValues.RTP_HEADER_SIZE;


public class RtpPacket {

    //Fields that compose the RTP header, refer https://dl.acm.org/doi/pdf/10.17487/RFC3550
    private static final int VERSION = 2; //version will remain constant, change if rtp version changes
    private static final int PADDING = 0;
    private static final int EXTENSION = 0; //no extension headers
    private static final int CC = 0; //no contributors since single source
    private int marker = 0; //depending on application logic, may be used to indicate end of audio stream
    private int payloadType;
    private int sequenceNumber;
    private int timeStamp;
    private int ssrc;

    //Bitstream of the RTP header
    private byte[] header;

    //size of the RTP payload
    private int payloadSize;
    //Bitstream of the RTP payload
    private byte[] payload;

    //--------------------------
    //Constructor of an RTPpacket object from header fields and payload bitstream
    //--------------------------
    public RtpPacket(int payloadType, int sequenceNumber, int timeStamp, int ssrc, int marker, byte[] data, int dataLength){

        //fill changing header fields:
        this.sequenceNumber = sequenceNumber;
        this.timeStamp = timeStamp;
        this.payloadType = payloadType;
        this.ssrc = ssrc;
        this.marker = marker;

        //build the header bistream:
        header = new byte[RTP_HEADER_SIZE];

        //fill the header array of byte with RTP header fields
        header[0] = (byte)(VERSION << 6 | PADDING << 5 | EXTENSION << 4 | CC);
        header[1] = (byte)(marker << 7 | this.payloadType & 0x000000FF);
        header[2] = (byte)((this.sequenceNumber & 0xFF00) >> 8);
        header[3] = (byte)(this.sequenceNumber & 0x00FF);
        header[4] = (byte)((this.timeStamp & 0xFF000000) >> 24);
        header[5] = (byte)((this.timeStamp & 0x00FF0000) >> 16);
        header[6] = (byte)((this.timeStamp & 0x0000FF00) >> 8);
        header[7] = (byte)(this.timeStamp & 0x000000FF);
        header[8] = (byte)((this.ssrc & 0xFF000000) >> 24);
        header[9] = (byte)((this.ssrc & 0x00FF0000) >> 16);
        header[10] = (byte)((this.ssrc & 0x0000FF00) >> 8);
        header[11] = (byte)(this.ssrc & 0x000000FF);

        //fill the payload bitstream:
        payloadSize = dataLength;
        payload = new byte[dataLength];

        //fill payload array of byte from data (given in parameter of the constructor)
        payload = Arrays.copyOf(data, payloadSize);
    }

    //--------------------------
    //Constructor of an RTPpacket object from the packet bistream
    //--------------------------
    public RtpPacket(byte[] packet, int packetSize)
    {

        //check if total packet size is lower than the header size
        if (packetSize >= RTP_HEADER_SIZE)
        {
            //get the header bitsream:
            header = new byte[RTP_HEADER_SIZE];
            System.arraycopy(packet, 0, header, 0, RTP_HEADER_SIZE);

            //get the payload bitstream:
            payloadSize = packetSize - RTP_HEADER_SIZE;
            payload = new byte[payloadSize];
            System.arraycopy(packet, RTP_HEADER_SIZE, payload, 0, packetSize - RTP_HEADER_SIZE);

            //interpret the changing fields of the header:
            marker = (header[1] & 0xFF & 0x80) >> 7;
            payloadType = header[1] & 0xFF & 0x7F;
            sequenceNumber = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
            timeStamp = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((header[4] & 0xFF) << 24);
            ssrc = (header[11] & 0xFF) + ((header[10] & 0xFF) << 8) + ((header[9] & 0xFF) << 16) + ((header[8] & 0xFF) << 24);
        }
    }

    public void getPayload(byte[] data) {

        if (payloadSize >= 0) System.arraycopy(payload, 0, data, 0, payloadSize);

    }

    public int getPayloadLength() {
        return(payloadSize);
    }

    public int getLength() {
        return(payloadSize + RTP_HEADER_SIZE);
    }

    public void getPacket(byte[] packet)
    {
        //construct the packet = header + payload
        if (RTP_HEADER_SIZE >= 0) System.arraycopy(header, 0, packet, 0, RTP_HEADER_SIZE);
        if (payloadSize >= 0) System.arraycopy(payload, 0, packet, RTP_HEADER_SIZE, payloadSize);

    }

    public int getTimeStamp() {
        return(timeStamp);
    }

    public int getSequenceNumber() {
        return(sequenceNumber);
    }

    public int getSsrc() {
        return ssrc;
    }

    public int getPayloadType() {
        return(payloadType);
    }

    public String getHeaderAsString()
    {
       return "Version:" + VERSION + " Padding:" + PADDING + " Extension: " + EXTENSION + " CC: " + CC + " Marker:" + marker
                     + " PayloadType:" + payloadType + " SequenceNumber:" + sequenceNumber + " TimeStamp:" + timeStamp;
    }
}