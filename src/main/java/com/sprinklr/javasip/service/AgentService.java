package com.sprinklr.javasip.service;

import com.sprinklr.javasip.agent.Agent;
import com.sprinklr.javasip.agent.AgentConfig;
import com.sprinklr.javasip.agent.AgentManager;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/*
Service class for Agent
 */
@Service
public class AgentService {

    private final ThreadPoolExecutor executor;
    private final AgentManager agentManager;
    private final Yaml yaml;
    private final static String YAML_CONFIG_DIR = "src/main/resources/yaml/";

    public AgentService() {
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        executor.setCorePoolSize(2); //set as per requirement, also add setMaximumPoolSize
        agentManager = new AgentManager();
        yaml = new Yaml();
    }

    public void startAgent(String id) throws IOException {
        InputStream ymlStream = Files.newInputStream(Paths.get(YAML_CONFIG_DIR + "agent" + id + ".yaml"));
        AgentConfig config = yaml.loadAs(ymlStream, AgentConfig.class);
        Agent agent = new Agent(config);
        agentManager.addAgent(agent, config);
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
