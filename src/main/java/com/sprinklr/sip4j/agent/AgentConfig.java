package com.sprinklr.sip4j.agent;

import java.util.UUID;

import static com.sprinklr.sip4j.utils.Constants.RTP_HEADER_SIZE;

/**
 * Defines the complete configuration of an Agent. Read from yaml file
 */
public class AgentConfig {

    private String getAgentName;
    private String transportMode;

    /*
    ---------------------------------------------- SIP CONFIG ------------------------------------------------
     */

    private String sipLocalIp;
    private int sipLocalPort;
    private String sipLocalUsername;
    private String sipLocalRealm;
    private String sipLocalDisplayName;

    private String sipRegistrarIp;
    private int sipRegistrarPort;

    private int sipRegisterExpiryTimeSec;
    private final String sipLocalTag = UUID.randomUUID().toString();

    /*
    ---------------------------------------------- RTP CONFIG ------------------------------------------------
     */

    private int rtpLocalPort;
    private String rtpLocalIp;

    private String rtpAddressType;
    private String rtpNetworkType;

    private int rtpPayloadSize;
    private int rtpPacketSize;

    /*
    ---------------------------------------------- WEBSOCKET CONFIG ------------------------------------------------
     */

    private String wsServerUri;


    /*
    ---------------------------------------------- MISC CONFIG ------------------------------------------------
     */
    private String password;

    public String getAgentName() {
        return getAgentName;
    }

    public void setAgentName(String agentName) {
        this.getAgentName = agentName;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public String getSipLocalIp() {
        return sipLocalIp;
    }

    public void setSipLocalIp(String sipLocalIp) {
        this.sipLocalIp = sipLocalIp;
    }

    public int getSipLocalPort() {
        return sipLocalPort;
    }

    public void setSipLocalPort(int sipLocalPort) {
        this.sipLocalPort = sipLocalPort;
    }

    public String getSipLocalUsername() {
        return sipLocalUsername;
    }

    public void setSipLocalUsername(String sipLocalUsername) {
        this.sipLocalUsername = sipLocalUsername;
    }

    public String getSipLocalRealm() {
        return sipLocalRealm;
    }

    public void setSipLocalRealm(String sipLocalRealm) {
        this.sipLocalRealm = sipLocalRealm;
    }

    public String getSipLocalDisplayName() {
        return sipLocalDisplayName;
    }

    public void setSipLocalDisplayName(String sipLocalDisplayName) {
        this.sipLocalDisplayName = sipLocalDisplayName;
    }

    public String getSipRegistrarIp() {
        return sipRegistrarIp;
    }

    public void setSipRegistrarIp(String sipRegistrarIp) {
        this.sipRegistrarIp = sipRegistrarIp;
    }

    public int getSipRegistrarPort() {
        return sipRegistrarPort;
    }

    public void setSipRegistrarPort(int sipRegistrarPort) {
        this.sipRegistrarPort = sipRegistrarPort;
    }

    public int getSipRegisterExpiryTimeSec() {
        return sipRegisterExpiryTimeSec;
    }

    public void setSipRegisterExpiryTimeSec(int sipRegisterExpiryTimeSec) {
        this.sipRegisterExpiryTimeSec = sipRegisterExpiryTimeSec;
    }

    public String getSipLocalTag() {
        return sipLocalTag;
    }

    public int getRtpLocalPort() {
        return rtpLocalPort;
    }

    public void setRtpLocalPort(int rtpLocalPort) {
        this.rtpLocalPort = rtpLocalPort;
    }

    public String getRtpLocalIp() {
        return rtpLocalIp;
    }

    public void setRtpLocalIp(String rtpLocalIp) {
        this.rtpLocalIp = rtpLocalIp;
    }

    public String getRtpAddressType() {
        return rtpAddressType;
    }

    public void setRtpAddressType(String rtpAddressType) {
        this.rtpAddressType = rtpAddressType;
    }

    public String getRtpNetworkType() {
        return rtpNetworkType;
    }

    public void setRtpNetworkType(String rtpNetworkType) {
        this.rtpNetworkType = rtpNetworkType;
    }

    public int getRtpPayloadSize() {
        return rtpPayloadSize;
    }

    public void setRtpPayloadSize(int rtpPayloadSize) {
        this.rtpPayloadSize = rtpPayloadSize;
        this.rtpPacketSize = RTP_HEADER_SIZE + rtpPayloadSize;
    }

    public int getRtpPacketSize() {
        return rtpPacketSize;
    }

    public String getWsServerUri() {
        return wsServerUri;
    }

    public void setWsServerUri(String wsServerUri) {
        this.wsServerUri = wsServerUri;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "AgentConfig2{" +
                "agentName='" + getAgentName + '\'' +
                ", transportMode='" + transportMode + '\'' +
                ", sipLocalIp='" + sipLocalIp + '\'' +
                ", sipLocalPort=" + sipLocalPort +
                ", sipLocalUsername='" + sipLocalUsername + '\'' +
                ", sipLocalRealm='" + sipLocalRealm + '\'' +
                ", sipLocalDisplayName='" + sipLocalDisplayName + '\'' +
                ", sipRegistrarIp='" + sipRegistrarIp + '\'' +
                ", sipRegistrarPort=" + sipRegistrarPort +
                ", sipRegisterExpiryTimeSec=" + sipRegisterExpiryTimeSec +
                ", sipLocalTag='" + sipLocalTag + '\'' +
                ", rtpLocalPort=" + rtpLocalPort +
                ", rtpLocalIp='" + rtpLocalIp + '\'' +
                ", rtpAddressType='" + rtpAddressType + '\'' +
                ", rtpNetworkType='" + rtpNetworkType + '\'' +
                ", rtpPayloadSize=" + rtpPayloadSize +
                ", rtpPacketSize=" + rtpPacketSize +
                ", wsServerUri='" + wsServerUri + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
