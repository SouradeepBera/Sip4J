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

/**
 * Agent's websocket entity which communicates for media transfer with voice bot websocket server
 */
public class Websocket extends WebSocketClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(Websocket.class);
    private final Queue<byte[]> outboundRtpQueue;
    private final AgentState agentState;
    private final AgentConfig agentConfig;

    /**
     * Constructs a WebSocketClient instance and sets it to the connect to the specified URI. The
     * channel does not attampt to connect automatically. The connection will be established once you
     * call connect(). Uses Draft 6455 for connection.
     *
     * @param outboundRtpQueue The queue where the messages from the voicebot are stored, which are to be sent via the RtpSender
     * @param agentState The state of the Agent to whom this websocket belongs.
     * @param agentConfig The configuration of the Agent to whom this websocket belongs
     * @throws URISyntaxException
     */
    public Websocket(Queue<byte[]> outboundRtpQueue, AgentState agentState, AgentConfig agentConfig) throws URISyntaxException {
        super(new URI(agentConfig.getWsServerUri()));
        this.outboundRtpQueue = outboundRtpQueue;
        this.agentState = agentState;
        this.agentConfig = agentConfig;
    }

    /**
     * Called after an opening handshake has been performed and the given websocket is ready to be
     * written on.
     *
     * @param serverHandShake The handshake of the websocket instance
     */
    @Override
    public void onOpen(ServerHandshake serverHandShake) {
        LOGGER.info("New connection opened for {} with HttpStatus:{} and HttpStatusMessage:{}",
                agentConfig.getAgentName(), serverHandShake.getHttpStatus(), serverHandShake.getHttpStatusMessage());
    }

    /**
     * Callback for string messages received from the remote host
     *
     * @param message The UTF-8 decoded message that was received.
     */
    @Override
    public void onMessage(String message) {
        //string message never sent by bot websocket server
    }

    /**
     * Callback for binary messages received from the remote host
     *
     * @param byteBuffer The binary message that was received.
     */
    @Override
    public void onMessage(ByteBuffer byteBuffer) {
        outboundRtpQueue.offer(byteBuffer.array());
    }

    /**
     * Called after the websocket connection has been closed.
     *
     * @param code   Websocket close code, refer <a href="https://github.com/Luka967/websocket-close-codes">...</a>
     * @param reason Additional information string
     * @param remote Returns if the closing of the connection was initiated by the remote host.
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("closed {} with exit code {} additional info: {}", agentConfig.getAgentName(), code, reason);
        agentState.setWsCloseCode(code);
    }

    /**
     * Called when errors occurs.
     *
     * @param ex The exception causing this error
     */
    @Override
    public void onError(Exception ex) {

        if (LOGGER.isErrorEnabled() && ex != null) {
            LOGGER.error("Error occurred in {}: {}", agentConfig.getAgentName(), ex.toString());
        }

    }
}
