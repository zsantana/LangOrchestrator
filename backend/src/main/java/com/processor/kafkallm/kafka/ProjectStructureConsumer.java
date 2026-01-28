package com.processor.kafkallm.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processor.kafkallm.model.ProcessingResult;
import com.processor.kafkallm.model.ProjectStructure;
import com.processor.kafkallm.service.LlmProcessingService;
import com.processor.kafkallm.service.StorageService;
import com.processor.kafkallm.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka que processa mensagens do tópico project-structure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectStructureConsumer {

    private final ObjectMapper objectMapper;
    private final LlmProcessingService llmProcessingService;
    private final StorageService storageService;
    private final WebSocketNotificationService notificationService;

    /**
     * Consome mensagens do tópico project-structure e processa com LLM.
     */
    @KafkaListener(
        topics = "${spring.kafka.topic.project-structure}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeProjectStructure(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Mensagem recebida - Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);
        log.info("Payload: {}", message);
        
        try {
            // Deserializar mensagem
            ProjectStructure projectStructure = objectMapper.readValue(message, ProjectStructure.class);
            
            log.info("Processando projeto: {} (ID: {})", 
                     projectStructure.getProjectName(), 
                     projectStructure.getProjectId());
            
            // Notificar início do processamento
            notificationService.notifyProcessingStarted(projectStructure.getProjectId());
            
            // Processar com LLM
            ProcessingResult result = llmProcessingService.processWithLlm(message);
            
            // Salvar resultado em arquivo
            String filePath = storageService.saveResult(result);
            log.info("Resultado salvo em: {}", filePath);
            
            // Notificar conclusão
            notificationService.notifyProcessingCompleted(result);
            
            log.info("Processamento concluído com sucesso para projeto: {}", 
                     projectStructure.getProjectId());
            
        } catch (Exception e) {
            log.error("Erro ao processar mensagem do Kafka: {}", e.getMessage(), e);
            
            // Tentar extrair projectId para notificação de erro
            try {
                ProjectStructure structure = objectMapper.readValue(message, ProjectStructure.class);
                notificationService.notifyProcessingError(
                    structure.getProjectId(),
                    e.getMessage()
                );
            } catch (Exception ex) {
                log.error("Erro ao enviar notificação de erro: {}", ex.getMessage());
            }
        }
    }
}
