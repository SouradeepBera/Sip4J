package com.sprinklr.sip4j.utils;

/**
 * Define values which remain constant throughout execution across all agents and all entities of an agent
 */
public class Constants {

    private Constants() {
        throw new IllegalStateException("Utility class");
    }
    public static final int RTP_HEADER_SIZE = 12;
    public static final long SLEEP_CPU_TIME_MS = 20;


}
