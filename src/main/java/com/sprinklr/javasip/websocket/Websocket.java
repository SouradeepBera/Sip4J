package com.sprinklr.javasip.websocket;

import com.sprinklr.javasip.agent.AgentConfig;
import com.sprinklr.javasip.agent.AgentState;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Queue;

/*
 * Agent's websocket entity which communicates for media transfer with bot
 */
public class Websocket extends WebSocketClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(Websocket.class);
    private final Queue<byte[]> outboundRtpQueue;
    private final AgentState agentState;
    private final AgentConfig agentConfig;

    public Websocket(Queue<byte[]> outboundRtpQueue, AgentState agentState, AgentConfig agentConfig) throws URISyntaxException {
        super(new URI(agentConfig.wsServerUri));
        this.outboundRtpQueue = outboundRtpQueue;
        this.agentState = agentState;
        this.agentConfig = agentConfig;
    }

    @Override
    public void onOpen(ServerHandshake serverHandShake) {
        LOGGER.info("New connection opened for {} with HttpStatus:{} and HttpStatusMessage:{}",
                agentConfig.agentName, serverHandShake.getHttpStatus(), serverHandShake.getHttpStatusMessage());
    }

    @Override
    public void onMessage(String message) {
        //string message never sent by bot websocket server
    }

    @Override
    public void onMessage(ByteBuffer byteBuffer) {
        outboundRtpQueue.offer(byteBuffer.array());
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("closed {} with exit code {} additional info: {}", agentConfig.agentName, code, reason);
        agentState.setWsCloseCode(code);
    }

    @Override
    public void onError(Exception ex) {

        if (LOGGER.isErrorEnabled() && ex != null) {
            LOGGER.error("Error occurred in {}: {}", agentConfig.agentName, ex.toString());
        }

    }
}
