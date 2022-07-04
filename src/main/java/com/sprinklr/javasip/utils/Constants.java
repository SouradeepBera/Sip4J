package com.sprinklr.javasip.utils;

import java.util.concurrent.TimeUnit;

/**
 * Define values which remain constant throughout execution across all Agents and all entities of an Agent
 */
public class Constants {

    /**
     * Private constructor since it is a utility class
     */
    private Constants() {
        throw new IllegalStateException("Utility class");
    }
    public static final int RTP_HEADER_SIZE = 12;
    public static final long SLEEP_CPU_TIME_MS = TimeUnit.MILLISECONDS.toMillis(20);


}
