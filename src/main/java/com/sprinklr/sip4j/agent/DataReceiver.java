package com.sprinklr.sip4j.agent;

/**
 * Any entity which receives data from Ozonetel for the Agent implements this interface
 */
public interface DataReceiver extends Runnable{

    /**
     * Starts the receiver
     */
    public void start();

    /**
     * Stop the receiver
     */
    public void stop();
}
