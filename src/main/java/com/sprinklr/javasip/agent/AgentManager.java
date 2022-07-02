package com.sprinklr.javasip.agent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AgentManager {
    private final Map<String, Agent> agentMapper = new ConcurrentHashMap<>();

    public void addAgent(Agent agent, AgentConfig agentConfig){
        agentMapper.put(agentConfig.agentName, agent);
    }

    public Agent getAgentByConfig(AgentConfig agentConfig){
        return agentMapper.get(agentConfig.agentName);
    }

    public Agent getAgentByName(String agentName){
        return agentMapper.get(agentName);
    }

    public void removeAgentByName(String agentName){
        agentMapper.remove(agentName);
    }

    public Set<String> getNames(){
        return agentMapper.keySet();
    }
}
