package com.sprinklr.javasip.sip;

import javax.sdp.SdpFactory;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;

/*
* Singleton which initialises sip+sdp factories
 */
public class SipAllFactories {
    private final SipFactory sipFactory;
    private final SdpFactory sdpFactory;
    private final AddressFactory addressFactory;
    private final MessageFactory messageFactory;
    private final HeaderFactory headerFactory;
    private static SipAllFactories singleInstance = null;

    private SipAllFactories() throws PeerUnavailableException {
        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        sdpFactory = SdpFactory.getInstance();
        headerFactory = sipFactory.createHeaderFactory();
        addressFactory = sipFactory.createAddressFactory();
        messageFactory = sipFactory.createMessageFactory();
    }

    public static SipAllFactories getInstance() throws PeerUnavailableException {
        if(singleInstance == null){
            singleInstance = new SipAllFactories();
        }
        return singleInstance;
    }

    public SipFactory getSipFactory() {
        return sipFactory;
    }

    public SdpFactory getSdpFactory() {
        return sdpFactory;
    }

    public AddressFactory getAddressFactory() {
        return addressFactory;
    }

    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    public HeaderFactory getHeaderFactory() {
        return headerFactory;
    }
}
