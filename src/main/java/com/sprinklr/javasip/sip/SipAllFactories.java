package com.sprinklr.javasip.sip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sdp.SdpFactory;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;

/**
 * Utility class which initialises sip+sdp factories
 */
public class SipAllFactories {

    private static final Logger LOGGER = LoggerFactory.getLogger(SipAllFactories.class);
    public static final SipFactory SIP_FACTORY = SipFactory.getInstance();
    static {
        SIP_FACTORY.setPathName("gov.nist");
    }

    public static final SdpFactory SDP_FACTORY = SdpFactory.getInstance();

    public static final AddressFactory ADDRESS_FACTORY;
    static {
        AddressFactory tmpAddressFactory;
        try {
            tmpAddressFactory = SIP_FACTORY.createAddressFactory();
        } catch (PeerUnavailableException e) {
            LOGGER.error("Peer Unavailable Exception for Address Factory, factory not initialised : {}", e.toString());
            tmpAddressFactory = null;
        }
        ADDRESS_FACTORY = tmpAddressFactory;
    }

    public static final MessageFactory MESSAGE_FACTORY;
    static {
        MessageFactory tmpMessageFactory;
        try {
            tmpMessageFactory = SIP_FACTORY.createMessageFactory();
        } catch (PeerUnavailableException e) {
            LOGGER.error("Peer Unavailable Exception for Message Factory, factory not initialised : {}", e.toString());
            tmpMessageFactory = null;
        }
        MESSAGE_FACTORY = tmpMessageFactory;
    }

    public static final HeaderFactory HEADER_FACTORY;

    static {
        HeaderFactory tmpHeaderFactory;
        try {
            tmpHeaderFactory = SIP_FACTORY.createHeaderFactory();
        } catch (PeerUnavailableException e) {
            LOGGER.error("Peer Unavailable Exception for Header Factory, factory not initialised : {}", e.toString());
            tmpHeaderFactory = null;
        }
        HEADER_FACTORY = tmpHeaderFactory;
    }

    private SipAllFactories() {
        throw new IllegalStateException("Utility class");
    }
}
