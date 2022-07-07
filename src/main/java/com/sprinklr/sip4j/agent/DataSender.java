package com.sprinklr.sip4j.agent;

/**
 * Any entity which sends data to Ozonetel from the Agent implements this interface
 */
public interface DataSender extends Runnable {

    /**
     * Starts the sender
     */
    void start();

    /**
     * Stops the sender
     */
    void stop();
}
