package com.sprinklr.javasip.controller;

import com.sprinklr.javasip.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/*
 * Controller class for Agent
 */
@RestController
@RequestMapping(value = "/agent")
public class AgentController {

    @Autowired
    AgentService agentService;

    @GetMapping(value = "/start/{id}")
    public void startAgent(@PathVariable("id") String id) throws IOException {
        agentService.startAgent(id);
    }

    @GetMapping(value = "/shutdown")
    public void shutdown() {
        agentService.shutdown();
    }

    @GetMapping(value = "/allStatus")
    public List<String> showAllStatus() {
        return agentService.showAllStatus();
    }
}
