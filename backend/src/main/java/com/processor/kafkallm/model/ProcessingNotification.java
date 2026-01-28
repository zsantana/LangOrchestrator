package com.processor.kafkallm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Modelo que representa uma notificação de processamento via WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingNotification {

    private String type;
    private String projectId;
    private String fileId;
    private String message;
    private LocalDateTime timestamp;
    private NotificationData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationData {
        private String status;
        private String filePath;
        private Long processingTimeMs;
        private String downloadUrl;
        private String error;
    }

    public enum NotificationType {
        PROCESSING_STARTED("processing_started"),
        PROCESSING_COMPLETED("processing_completed"),
        PROCESSING_ERROR("processing_error");

        private final String value;

        NotificationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
