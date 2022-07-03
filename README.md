## Introduction

---

### What it does?

**javasip** was developed for replacing JsSip in Java, using SIP+SDP for signalling and RTP+Websocket
for media transfer.\
Although **javasip** was specifically designed for the Sprinklr usecase, it can very easily be
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

### Creating an AgentConfig

An agent configuration is created using the ```AgentConfig``` class. It can be created in multiple ways
, from a Json passed in a POST request to run an Agent or internally from config files.
If there are passwords or sensitive information, internal config files can be used.

---

### Starting an Agent

An Agent can be started in blocking as well as non-blocking mode.
To start an Agent in blocking mode, pass an AgentConfig and call the start() method

In blocking mode,
```java
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

```java
Agent agent = new Agent(agentConfig);
agentManager.addAgent(agent, agentConfig);
executor.submit(agent);
```

For retrieval and deletion refer to the ```AgentManager``` class.

---

### Example
The mockserver package consists of mock clients and servers which mimic the foreign entities
and allow you to run the code. 
It sends audiobytes by reading from a local file, and completes the flow code by reconstructing
the same file after passing it through all the components.
Ensure the port numbers are setup correctly in the mockserver package.

To run it start the SpringBoot application. Start the WsBot server, SipOzonetel client and create an Agent
by passing the config in the POST body like so
```json
{
  "transportMode":"udp", 
  "agentName":"Agent_1",
  "sipLocalIp":"127.0.0.1",
  "sipLocalPort":"5070",
  "sipLocalUsername":"souradeep.bera",
  "sipLocalRealm":"sprinklr.com",
  "sipLocalDisplayName":"soura",
  "sipRegistrarIp":"127.0.0.1",
  "sipRegistrarPort":"5060",
  "sipRegisterExpiryTimeSec":"3600",
  "rtpLocalPort":"6022",
  "rtpLocalIp":"192.168.1.8",
  "rtpAddressType":"IP4",
  "rtpNetworkType":"IN",
  "rtpPayloadSize":"256",
  "wsServerUri":"ws://localhost:8887",
  "password":"password12345",
}
```

To send and receive audio packets start the RtpOzonetelReceiver followed by the RtpOzonetelSender

---