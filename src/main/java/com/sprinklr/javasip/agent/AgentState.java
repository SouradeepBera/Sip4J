package com.sprinklr.javasip.agent;


import com.sprinklr.javasip.sip.SipState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class AgentState {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentState.class);
    private final String name;
    private volatile String sipState = SipState.UNREGISTERED;
    private volatile int wsCloseCode = 0;

    public AgentState(String name) {
        this.name = name;
    }

    public int getWsCloseCode() {
        return wsCloseCode;
    }

    public void setWsCloseCode(int wsCloseCode) {
        LOGGER.info("{}'s wsCloseCode set to {}", name, wsCloseCode);
        this.wsCloseCode = wsCloseCode;
    }

    public String getSipState() {
        return sipState;
    }

    public void setSipState(String sipState) {
        LOGGER.info("{}'s sipState set to {}", name, sipState);
        this.sipState = sipState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentState that = (AgentState) o;
        return wsCloseCode == that.wsCloseCode && name.equals(that.name) && sipState.equals(that.sipState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sipState, wsCloseCode);
    }

    @Override
    public String toString() {
        return "AgentState{" +
                "name='" + name + '\'' +
                ", sipState='" + sipState + '\'' +
                ", wsCloseCode=" + wsCloseCode +
                '}';
    }
}
