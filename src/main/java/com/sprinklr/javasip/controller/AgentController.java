package com.sprinklr.javasip.controller;

import com.sprinklr.javasip.agent.Agent;
import com.sprinklr.javasip.agent.AgentConfig;
import com.sprinklr.javasip.agent.AgentManager;
import com.sprinklr.javasip.agent.AgentState;
import com.sprinklr.javasip.sip.SipState;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.sprinklr.javasip.utils.ConstantValues.sipAllowedMethods;

@RestController
@RequestMapping(value = "/agent")
public class AgentController {

    ThreadPoolExecutor executor;
    AgentManager agentManager;

    public AgentController() {
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        executor.setCorePoolSize(2);
        agentManager = new AgentManager();
    }

    @RequestMapping(value = "/start", method = RequestMethod.POST)
    public void startAgent(@RequestBody Map<String, Object> body){

        Map<String, String> config = new HashMap<>();
        for(String key : body.keySet()) {
            String val = (String) body.get(key);
            config.put(key, val);
        }

        AgentConfig agentConfig = new AgentConfig.Builder(config.get("transportMode"), sipAllowedMethods, config.get("password"), config.get("agentName"))
                .sipConfig(config.get("sipLocalIp"), Integer.parseInt(config.get("sipLocalPort")), config.get("sipLocalUsername"),config.get("sipLocalRealm"),
                        config.get("sipLocalDisplayName"), config.get("sipRegistrarIp"), Integer.parseInt(config.get("sipRegistrarPort")), Integer.parseInt(config.get("sipRegisterExpiryTimeSec")))
                .rtpConfig(config.get("rtpLocalIp"), Integer.parseInt(config.get("rtpLocalPort")), config.get("rtpAddressType"),
                        config.get("rtpNetworkType"),  Integer.parseInt(config.get("rtpPayloadSize")))
                .wsConfig(config.get("wsServerUri"))
                .build();

        Agent agent = new Agent(agentConfig);
        agentManager.addAgent(agent, agentConfig);
        executor.submit(agent);
    }

    @RequestMapping(value = "/shutdown", method = RequestMethod.GET)
    public void shutdown(){
        executor.shutdown();
    }

    @RequestMapping(value = "/allStatus", method = RequestMethod.GET)
    public List<String> showAllStatus(){
        List<String> statuses = new ArrayList<>();
        for(String agentName : agentManager.getNames()){
            String status = agentName + " " + agentManager.getAgentByName(agentName).getState();
            statuses.add(status);
        }
        return statuses;
    }

    @RequestMapping(value = "/reconnectDisconnected", method = RequestMethod.GET)
    public List<String> reconnectDisconnected(){
        List<String> infoMsgList = new ArrayList<>();
        for(String agentName : agentManager.getNames()){
            Agent agent = agentManager.getAgentByName(agentName);
            AgentState agentState = agent.getState();
            AgentConfig agentConfig = agent.getConfig();
           if(SipState.DISCONNECTED.equals(agentState.getSipState())){
               agent.shutdown();
               agentManager.removeAgentByName(agentName);

               Agent newAgent = new Agent(agentConfig);
               agentManager.addAgent(newAgent, agentConfig);
               executor.submit(newAgent);
               infoMsgList.add("Reconnected " + agentName);
           }
        }
        return infoMsgList;
    }
}
