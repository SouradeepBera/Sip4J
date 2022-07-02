package com.sprinklr.javasip.utils;

import javax.sip.message.Request;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*
 * Define values which remain constant throughout
 */
public class Constants {

    private Constants() {
        throw new IllegalStateException("Utility class");
    }

    public static final List<String> SIP_ALLOWED_METHODS = Collections.unmodifiableList(Arrays.asList(Request.INVITE, Request.BYE, Request.CANCEL, Request.ACK));
    public static final int RTP_HEADER_SIZE = 12;
    public static final int RTP_BLOCK_SOCKET_TIME_MS = (int) TimeUnit.MILLISECONDS.toMillis(1000);
    public static final int WS_RECONNECT_CODE = 1006;
    public static final long SLEEP_CPU_TIME_MS = TimeUnit.MILLISECONDS.toMillis(20);


}
