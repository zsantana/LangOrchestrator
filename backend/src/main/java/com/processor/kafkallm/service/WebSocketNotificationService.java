package com.processor.kafkallm.service;

import com.processor.kafkallm.model.ProcessingNotification;
import com.processor.kafkallm.model.ProcessingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Serviço responsável por enviar notificações via WebSocket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notifica início do processamento.
     */
    public void notifyProcessingStarted(String projectId) {
        log.info("Enviando notificação de início: {}", projectId);
        
        ProcessingNotification notification = ProcessingNotification.builder()
            .type(ProcessingNotification.NotificationType.PROCESSING_STARTED.getValue())
            .projectId(projectId)
            .message("Processamento iniciado")
            .timestamp(LocalDateTime.now())
            .data(ProcessingNotification.NotificationData.builder()
                .status("STARTED")
                .build())
            .build();
        
        sendNotification(notification);
    }

    /**
     * Notifica conclusão do processamento.
     */
    public void notifyProcessingCompleted(ProcessingResult result) {
        log.info("Enviando notificação de conclusão: {}", result.getProjectId());
        
        ProcessingNotification notification = ProcessingNotification.builder()
            .type(ProcessingNotification.NotificationType.PROCESSING_COMPLETED.getValue())
            .projectId(result.getProjectId())
            .fileId(result.getFileId())
            .message("Processamento concluído com sucesso")
            .timestamp(LocalDateTime.now())
            .data(ProcessingNotification.NotificationData.builder()
                .status(result.getStatus())
                .processingTimeMs(result.getProcessingTimeMs())
                .filePath(result.getFilePath())
                .build())
            .build();
        
        sendNotification(notification);
    }

    /**
     * Notifica erro no processamento.
     */
    public void notifyProcessingError(String projectId, String error) {
        log.error("Enviando notificação de erro: {}", projectId);
        
        ProcessingNotification notification = ProcessingNotification.builder()
            .type(ProcessingNotification.NotificationType.PROCESSING_ERROR.getValue())
            .projectId(projectId)
            .message("Erro durante o processamento")
            .timestamp(LocalDateTime.now())
            .data(ProcessingNotification.NotificationData.builder()
                .status("ERROR")
                .error(error)
                .build())
            .build();
        
        sendNotification(notification);
    }

    /**
     * Envia notificação para o tópico WebSocket.
     */
    private void sendNotification(ProcessingNotification notification) {
        try {
            // Enviar para tópico geral
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            
            // Enviar para tópico específico do projeto
            messagingTemplate.convertAndSend(
                "/topic/project/" + notification.getProjectId(),
                notification
            );
            
            log.debug("Notificação enviada: {}", notification);
            
        } catch (Exception e) {
            log.error("Erro ao enviar notificação WebSocket: {}", e.getMessage(), e);
        }
    }
}
