package com.processor.kafkallm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Aplicação principal do processador Kafka + LLM.
 * 
 * Funcionalidades:
 * - Consumidor Kafka (tópico: project-structure)
 * - Processamento com Anthropic LLM
 * - Salvamento de resultados em arquivo
 * - Notificação via WebSocket
 * - API REST para download
 */
@SpringBootApplication
@EnableAsync
public class LlmProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmProcessorApplication.class, args);
    }
}
