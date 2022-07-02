package com.sprinklr.javasip.sip;

/*
* Defines constants for state of sip entity
 */
public class SipState {

    private SipState(){
        throw new IllegalStateException("Utility class");
    }

    public static final String CONNECTING = "CONNECTING";

    public static final String CONNECTED = "CONNECTED";

    public static final String DISCONNECTED = "DISCONNECTED";

    public static final String REGISTERED = "REGISTERED";

    public static final String UNREGISTERED = "UNREGISTERED";

    public static final String REGISTRATION_FAILED = "REGISTRATION_FAILED";

}
