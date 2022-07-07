package com.sprinklr.sip4j.controller;

import com.sprinklr.sip4j.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Controller class for Agent
 */
@RestController
@RequestMapping(value = "/agent")
public class AgentController {

    @Autowired
    AgentService agentService;

    /**
     * Calls the servie for starting an agent by id
     * @param id id of the agent to be started
     * @throws IOException
     */
    @GetMapping(value = "/start/{id}")
    public void startAgent(@PathVariable("id") String id) throws IOException {
        agentService.startAgent(id);
    }

    /**
     * Calls the service for shutting down the executor service. No more Agents can be started once this is called
     */
    @GetMapping(value = "/shutdown")
    public void shutdown() {
        agentService.shutdown();
    }

    /**
     * Calls the service to show statuses of all active agents
     * @return The statuses of all active agents
     */
    @GetMapping(value = "/allStatus")
    public List<String> showAllStatus() {
        return agentService.showAllStatus();
    }
}
