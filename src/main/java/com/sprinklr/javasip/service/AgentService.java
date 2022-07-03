package com.sprinklr.javasip.service;

import com.sprinklr.javasip.agent.Agent;
import com.sprinklr.javasip.agent.AgentConfig;
import com.sprinklr.javasip.agent.AgentManager;
import com.sprinklr.javasip.sip.SipState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.sprinklr.javasip.utils.Constants.SIP_ALLOWED_METHODS;

/*
Service class for Agent
 */
@Service
public class AgentService {

    ThreadPoolExecutor executor;
    AgentManager agentManager;

    public AgentService() {
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        executor.setCorePoolSize(2); //set as per requirement, also add setMaximumPoolSize
        agentManager = new AgentManager();
    }

    public void startAgent(Map<String, Object> body){
        Map<String, String> config = new HashMap<>();
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            config.put(entry.getKey(), (String) entry.getValue());
        }

        AgentConfig agentConfig = new AgentConfig.Builder(config.get("transportMode"), SIP_ALLOWED_METHODS, config.get("password"), config.get("agentName"))
                .sipConfig(config.get("sipLocalIp"), Integer.parseInt(config.get("sipLocalPort")), config.get("sipLocalUsername"), config.get("sipLocalRealm"),
                        config.get("sipLocalDisplayName"), config.get("sipRegistrarIp"), Integer.parseInt(config.get("sipRegistrarPort")), Integer.parseInt(config.get("sipRegisterExpiryTimeSec")))
                .rtpConfig(config.get("rtpLocalIp"), Integer.parseInt(config.get("rtpLocalPort")), config.get("rtpAddressType"),
                        config.get("rtpNetworkType"), Integer.parseInt(config.get("rtpPayloadSize")))
                .wsConfig(config.get("wsServerUri"))
                .build();

        if(agentManager.containsAgent(agentConfig.agentName)){
            Agent prevAgentInst = agentManager.getAgentByName(agentConfig.agentName);
            if(!SipState.DISCONNECTED.equals(prevAgentInst.getState().getSipState())) {
                throw new IllegalStateException("Previous instance of agent still not disconnected from call");
            }
            prevAgentInst.clear();
            agentManager.removeAgentByName(agentConfig.agentName);
        }

        Agent agent = new Agent(agentConfig);
        agentManager.addAgent(agent, agentConfig);
        executor.submit(agent);
    }

    public List<String> showAllStatus() {
        List<String> statuses = new ArrayList<>();
        for (String agentName : agentManager.getNames()) {
            String status = agentName + " " + agentManager.getAgentByName(agentName).getState();
            statuses.add(status);
        }
        return statuses;
    }

    public void shutdown() {
        executor.shutdown();
    }


}
