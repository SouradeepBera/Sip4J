package com.sprinklr.javasip.rtp;

import java.util.Objects;

/**
 * Describes a complete RTP address
 */
public class RtpAddress {

    private final int port;
    private final String address;
    private final String addressType;
    private final String networkType;

    /**
     * Creates a complete RTP address
     * @param port the RTP port
     * @param address the RTP IP address
     * @param addressType the type of IP address (e.g. IP4 or IP6)
     * @param networkType the network type utilised
     */
    public RtpAddress(int port, String address, String addressType, String networkType) {
        this.port = port;
        this.address = address;
        this.addressType = addressType;
        this.networkType = networkType;
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }

    public String getAddressType() {
        return addressType;
    }

    public String getNetworkType() {
        return networkType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RtpAddress that = (RtpAddress) o;
        return port == that.port && address.equals(that.address) && addressType.equals(that.addressType) && networkType.equals(that.networkType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, address, addressType, networkType);
    }

    @Override
    public String toString() {
        return "RtpAddress{" +
                "port=" + port +
                ", address='" + address + '\'' +
                ", addressType='" + addressType + '\'' +
                ", networkType='" + networkType + '\'' +
                '}';
    }
}
