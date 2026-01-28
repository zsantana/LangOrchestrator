package com.processor.kafkallm.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * Controller WebSocket para comunicação bidirecional.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    /**
     * Endpoint para ping/pong (teste de conexão).
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String handlePing(String message) {
        log.debug("Ping recebido: {}", message);
        return "pong: " + System.currentTimeMillis();
    }

    /**
     * Endpoint para subscrição de notificações.
     */
    @MessageMapping("/subscribe")
    @SendTo("/topic/subscribed")
    public String handleSubscription(String clientId) {
        log.info("Cliente subscrito: {}", clientId);
        return "Subscrito com sucesso: " + clientId;
    }
}
