package com.sprinklr.javasip.controller;

import com.sprinklr.javasip.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/*
 * Controller class for Agent
 */
@RestController
@RequestMapping(value = "/agent")
public class AgentController {

    @Autowired
    AgentService agentService;

    @PostMapping(value = "/start")
    public void startAgent(@RequestBody Map<String, Object> body) {
        agentService.startAgent(body);
    }

    @GetMapping(value = "/shutdown")
    public void shutdown() {
        agentService.shutdown();
    }

    @GetMapping(value = "/allStatus")
    public List<String> showAllStatus() {
        return agentService.showAllStatus();
    }

    @GetMapping(value = "/reconnectDisconnected")
    public List<String> reconnectDisconnected() {
        return agentService.reconnectDisconnected();
    }
}
