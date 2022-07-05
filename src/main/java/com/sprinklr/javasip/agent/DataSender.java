package com.sprinklr.javasip.agent;

/**
 * Any entity which sends data to Ozonetel from the Agent implements this interface
 */
public interface DataSender extends Runnable {

    /**
     * Starts the sender
     */
    public void start();

    /**
     * Stops the sender
     */
    public void stop();
}
