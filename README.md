## Introduction

---

### What it does?

**javasip** was developed for replacing JsSip in Java, using SIP+SDP for signalling and RTP+Websocket
for media transfer.\
ALthough **javasip** was specifically designed for the Sprinklr usecase, it can very easily be
modified and extended to create a high-level sip library for Java.

---

### How does it work?

Ozonetel communicates with the Agent using SIP+SDP and sets up an RTP session between the two.
The media packets received by the Agent are then forwarded to the VoiceBot. Once the proccessing is done
on the VoiceBot's end, it sends the processed packets via websocket to the Agent which again communicates it
back to Ozonetel using the RTP session.

![Alt text](diagrams/Agent%20Breakdown.jpg)
![Alt text](diagrams/Component%20Breakdown.jpg)

---

## Using javasip

---

### Starting an Agent

An Agent can be started in blocking as well as non-blocking mode.
To start an Agent in blocking mode, pass an AgentConfig and call the start() method

In blocking mode,
```java
import com.sprinklr.javasip.agent.AgentConfig;

public class StartAgentBlocking() {
    
    AgentConfig agentConfig;
    
    StartAgentBlocking(AgentConfig agentConfig){
        this.agentConfig = agentConfig;
    }

    public static void main(String[] args) {
        Agent agent = new Agent(agentConfig);
        agent.start();
    }
}
```
In non-blocking mode,
```java
import com.sprinklr.javasip.agent.AgentConfig;

public class StartAgentNonBlocking() {
    
    AgentConfig agentConfig;
    
    StartAgentBlocking(AgentConfig agentConfig){
        this.agentConfig = agentConfig;
    }

    public static void main(String[] args) {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        executor.setCorePoolSize(1);
        executor.setMaximumPoolSize(1);
        Agent agent = new Agent(agentConfig);
        executor.submit(agent);
    }
}
```

---

### Configuring an AgentManager
When dealing with multiple agents, it might be helpful to keep track of the Agents along with their states
and configurations.
Agent encapsulates its corresponding state and configuration. Storing the Agent itself mapped by
it's name is sufficient to provide the required information.