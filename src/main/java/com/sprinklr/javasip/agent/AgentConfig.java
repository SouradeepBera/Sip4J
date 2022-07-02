package com.sprinklr.javasip.agent;

import java.util.List;
import java.util.UUID;

import static com.sprinklr.javasip.utils.ConstantValues.RTP_HEADER_SIZE;


public class AgentConfig {

    public final String agentName;
    public final String transportMode;

    /*
    ---------------------------------------------- SIP CONFIG ------------------------------------------------
     */

    public final String sipLocalIp;
    public final int sipLocalPort;
    public final String sipLocalUsername;
    public final String sipLocalRealm;
    public final String sipLocalDisplayName;

    public final String sipRegistrarIp;
    public final int sipRegistrarPort;

    public final int sipRegisterExpiryTimeSec;
    public final List<String> sipAllowedMethods;
    public final String sipLocalTag;

    /*
    ---------------------------------------------- RTP CONFIG ------------------------------------------------
     */

    public final int rtpLocalPort;
    public final String rtpLocalIp;

    public final String rtpAddressType;
    public final String rtpNetworkType;

    public final int rtpPayloadSize;
    public final int rtpPacketSize;

    /*
    ---------------------------------------------- WEBSOCKET CONFIG ------------------------------------------------
     */

    public final String wsServerUri;


    /*
    ---------------------------------------------- MISC CONFIG ------------------------------------------------
     */
    public final String password;

    public static class Builder{

        private final String agentName;
        private final String transportMode;
        private final String password;

        private String sipLocalIp;
        private int sipLocalPort;
        private String sipLocalUsername;
        private String sipLocalRealm;
        private String sipLocalDisplayName;

        private String sipRegistrarIp;
        private int sipRegistrarPort;

        private int sipRegisterExpiryTimeSec;
        private final List<String> sipAllowedMethods;


        private int rtpLocalPort;
        private String rtpLocalIp;

        private String rtpAddressType;
        private String rtpNetworkType;
        private int rtpPayloadSize;
        private int rtpPacketSize;

        private String wsServerUri;


        public Builder(String transportMode, List<String> sipAllowedMethods, String password, String agentName) {
            this.transportMode = transportMode;
            this.sipAllowedMethods = sipAllowedMethods;
            this.password = password;
            this.agentName = agentName;
        }

        public Builder sipConfig(String sipLocalIp, int sipLocalPort, String sipLocalUsername, String sipLocalRealm, String sipLocalDisplayName, String sipRegistrarIp, int sipRegistrarPort, int sipRegisterExpiryTimeSec){
            this.sipLocalIp = sipLocalIp;
            this.sipLocalPort = sipLocalPort;
            this.sipLocalUsername = sipLocalUsername;
            this.sipLocalRealm = sipLocalRealm;
            this.sipLocalDisplayName = sipLocalDisplayName;
            this.sipRegistrarIp = sipRegistrarIp;
            this.sipRegistrarPort = sipRegistrarPort;
            this.sipRegisterExpiryTimeSec = sipRegisterExpiryTimeSec;
            return this;
        }

        public Builder rtpConfig(String rtpLocalIp, int rtpLocalPort, String rtpAddressType, String rtpNetworkType, int rtpPayloadSize){
            this.rtpLocalIp = rtpLocalIp;
            this.rtpLocalPort = rtpLocalPort;
            this.rtpAddressType = rtpAddressType;
            this.rtpNetworkType = rtpNetworkType;
            this.rtpPayloadSize = rtpPayloadSize;
            this.rtpPacketSize = RTP_HEADER_SIZE + rtpPayloadSize;
            return this;
        }

        public Builder wsConfig(String wsServerUri){
            this.wsServerUri = wsServerUri;
            return this;
        }

        public AgentConfig build(){
            return new AgentConfig(this);
        }
    }

    private AgentConfig(Builder builder){
        agentName = builder.agentName;
        transportMode = builder.transportMode;

        sipLocalIp = builder.sipLocalIp;
        sipLocalPort = builder.sipLocalPort;
        sipLocalRealm = builder.sipLocalRealm;
        sipLocalUsername = builder.sipLocalUsername;
        sipLocalDisplayName = builder.sipLocalDisplayName;
        sipRegistrarIp = builder.sipRegistrarIp;
        sipRegistrarPort = builder.sipRegistrarPort;
        sipRegisterExpiryTimeSec = builder.sipRegisterExpiryTimeSec;
        sipAllowedMethods = builder.sipAllowedMethods;
        sipLocalTag = UUID.randomUUID().toString();

        rtpLocalIp = builder.rtpLocalIp;
        rtpLocalPort = builder.rtpLocalPort;
        rtpAddressType = builder.rtpAddressType;
        rtpNetworkType = builder.rtpNetworkType;
        rtpPayloadSize = builder.rtpPayloadSize;
        rtpPacketSize = builder.rtpPacketSize;

        wsServerUri = builder.wsServerUri;

        password = builder.password;
    }
}
