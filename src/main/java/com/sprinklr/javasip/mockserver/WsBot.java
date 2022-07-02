package com.sprinklr.javasip.mockserver;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/*
Mocks the bot websocket server which receives data from Agent's Websocket Client Entity
 */
public class WsBot extends WebSocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WsBot.class);

    /*
    Hardcoded values, will depend on voicebot, set here just to mimic actual flow
     */
    private static final int WS_BOT_PORT = 8887;    //not set by us
    private static final String WS_BOT_IP = "localhost";   //not set by us
    /*
    Hardcoded values end
     */

    public WsBot(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOGGER.info("new connection to {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        LOGGER.info("closed {} with exit code {}, additional info: {}", conn.getRemoteSocketAddress() , code, reason);
        Thread stopServerThread = new Thread(()-> {
            try {
                LOGGER.info("Stopping bot websocket server thread.");
                stop();
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupting bot websocket server thread");
                Thread.currentThread().interrupt();
            }
        });
        stopServerThread.start();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        //bot websocket server receives only byte data from websocket client entity
    }

    @Override
    public void onMessage( WebSocket conn, ByteBuffer message ) {
        conn.send(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.error("an error occurred on connection {} : {}", conn.getRemoteSocketAddress(), ex);
    }

    @Override
    public void onStart() {
        LOGGER.info("server started successfully");
    }

    public static void main(String[] args) {

        WebSocketServer server = new WsBot(new InetSocketAddress(WS_BOT_IP, WS_BOT_PORT));
        server.run();
    }
}