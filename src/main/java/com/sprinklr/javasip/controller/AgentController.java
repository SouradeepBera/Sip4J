package com.sprinklr.javasip.controller;

import com.sprinklr.javasip.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

    @RequestMapping(value = "/start", method = RequestMethod.POST)
    public void startAgent(@RequestBody Map<String, Object> body) {
        agentService.startAgent(body);
    }

    @RequestMapping(value = "/shutdown", method = RequestMethod.GET)
    public void shutdown() {
        agentService.shutdown();
    }

    @RequestMapping(value = "/allStatus", method = RequestMethod.GET)
    public List<String> showAllStatus() {
        return agentService.showAllStatus();
    }

    @RequestMapping(value = "/reconnectDisconnected", method = RequestMethod.GET)
    public List<String> reconnectDisconnected() {
        return agentService.reconnectDisconnected();
    }
}
