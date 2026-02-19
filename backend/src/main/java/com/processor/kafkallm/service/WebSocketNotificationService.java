package com.processor.kafkallm.service;

import com.processor.kafkallm.model.ProcessingNotification;
import com.processor.kafkallm.model.ProcessingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

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
        log.info("### Enviando notificação de início: {}", projectId);
        
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
        log.info("### Enviando notificação de conclusão: {}", result.getProjectId());

        String downloadUrl = null;
        if (result.getFileId() != null && !result.getFileId().isBlank()) {
            downloadUrl = "/api/v1/results/" + result.getFileId() + "/download";
        }
        
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
                .downloadUrl(downloadUrl)
                .promptTokens(result.getPromptTokens())
                .generationTokens(result.getGenerationTokens())
                .totalTokens(result.getTotalTokens())
                .build())
            .build();
        
        sendNotification(notification);
    }

    /**
     * Notifica erro no processamento.
     */
    public void notifyProcessingError(String projectId, String error) {
        log.error("### Enviando notificação de erro: {}", projectId);
        
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
     * Notifica processamento de upload com dados ficticios.
     */
    @Async
    public void notifyUploadProcessed(String fileId) {

        Map<String, Object> payload = Map.of(
            "id_key_processor", fileId,
            "url_download_file", "/api/v1/uploaded-files/" + fileId + "/download",
            "tot_token_input", 1000,
            "tot_token_output", 500,
            "tot_final", 1500,
            "timestamp", System.currentTimeMillis(),
            "status", "PROCESSADO COM SUCESSO"
        );

        sendRawNotification(payload, fileId);
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

    private void sendRawNotification(Map<String, Object> payload, String projectId) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", payload);
            messagingTemplate.convertAndSend("/topic/project/" + projectId, payload);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação WebSocket: {}", e.getMessage(), e);
        }
    }
}
