package com.sprinklr.sip4j.sip;

public class SipIncorrectAuthenticationSchemeException extends RuntimeException{
    public SipIncorrectAuthenticationSchemeException(String errorMessage) {
        super(errorMessage);
    }
}
