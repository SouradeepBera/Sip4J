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

/**
Service class for Agent
 */
@Service
public class AgentService {

    private static final String YAML_CONFIG_DIR = "src/main/resources/yaml/";
    private static final int N_AGENTS = 3;
    private final ThreadPoolExecutor executor;
    private final AgentManager agentManager;
    private final Yaml yaml;

    /**
     * Initialises the member variables. Assigns a ThreadPoolExecutor, AgentManager and Yaml for the service
     */
    public AgentService() {
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        executor.setCorePoolSize(N_AGENTS);
        executor.setMaximumPoolSize(N_AGENTS);
        agentManager = new AgentManager();
        yaml = new Yaml();
    }

    /**
     * Starts agent by id
     * @param id id of the agent to be started
     * @throws IOException
     */
    public void startAgent(String id) throws IOException {
        InputStream ymlStream = Files.newInputStream(Paths.get(YAML_CONFIG_DIR + "agent" + id + ".yaml"));
        AgentConfig config = yaml.loadAs(ymlStream, AgentConfig.class);
        Agent agent = new Agent(config);
        agentManager.addAgent(agent, config);
        executor.submit(agent);
    }

    /**
     * Shows statuses of all active agents
     * @return The statuses of all active agents
     */
    public List<String> showAllStatus() {
        List<String> statuses = new ArrayList<>();
        for (String agentName : agentManager.getNames()) {
            String status = agentName + " " + agentManager.getAgentByName(agentName).getState();
            statuses.add(status);
        }
        return statuses;
    }

    /**
     * Shuts down executor service. No more Agents can be started once this is called
     */
    public void shutdown() {
        executor.shutdown();
    }


}
