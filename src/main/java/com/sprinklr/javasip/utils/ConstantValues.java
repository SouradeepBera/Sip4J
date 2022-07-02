package com.sprinklr.javasip.utils;

import javax.sip.message.Request;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
 * Define values which remain constant throughout
 */
public class ConstantValues {

    private ConstantValues() {
        throw new IllegalStateException("Utility class");
    }

    public static final List<String> SIP_ALLOWED_METHODS = Collections.unmodifiableList(Arrays.asList(Request.INVITE, Request.BYE, Request.CANCEL, Request.ACK));
    public static final int RTP_HEADER_SIZE = 12;


}
