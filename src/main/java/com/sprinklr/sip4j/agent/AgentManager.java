package com.sprinklr.sip4j.agent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager class. Stores agents mapped by agent names
 */
public class AgentManager {
    private final Map<String, Agent> agentMapper;

    public AgentManager() {
        agentMapper = new ConcurrentHashMap<>();
    }

    public void addAgent(Agent agent, AgentConfig agentConfig) {
        agentMapper.put(agentConfig.getAgentName(), agent);
    }

    public Agent getAgentByName(String agentName) {
        return agentMapper.get(agentName);
    }

    public void removeAgentByName(String agentName) {
        agentMapper.remove(agentName);
    }

    public boolean containsAgent(String agentName){
        return agentMapper.containsKey(agentName);
    }

    public Set<String> getNames() {
        return agentMapper.keySet();
    }
}
