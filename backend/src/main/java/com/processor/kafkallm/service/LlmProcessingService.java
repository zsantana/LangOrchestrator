package com.processor.kafkallm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processor.kafkallm.config.AppConfig;
import com.processor.kafkallm.model.ProcessingResult;
import com.processor.kafkallm.model.ProjectStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço responsável pelo processamento com LLM (Anthropic Claude).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmProcessingService {

    private final AnthropicChatModel chatModel;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    /**
     * Processa a estrutura do projeto usando o LLM.
     */
    public ProcessingResult processWithLlm(String projectStructure) {
        return processWithLlmPrompt(projectStructure, null);
    }

    /**
     * Processa a estrutura do projeto usando o LLM.
     */
    public ProcessingResult processWithLlm(ProjectStructure projectStructure) {
        String userPrompt;
        try {
            userPrompt = objectMapper.writeValueAsString(projectStructure);
        } catch (Exception e) {
            userPrompt = "Estrutura do projeto: "
                + (projectStructure != null ? projectStructure.getProjectName() : "desconhecido");
        }
        return processWithLlmPrompt(userPrompt, projectStructure);
    }

    private ProcessingResult processWithLlmPrompt(String userPrompt, ProjectStructure projectStructure) {
        long startTime = System.currentTimeMillis();
        Long promptTokens = null;
        Long generationTokens = null;
        Long totalTokens = null;
        
        try {
            // Criar mensagens

            // log.info("### System prompt carregado: {}", appConfig.getLlm().getSystemPrompt());
            List<Message> messages = List.of(
                new SystemMessage(montarSystemPrompt()),
                new UserMessage(userPrompt)
            );
            
            log.info("### Executando chamada para LLM com modelo: {}, temperatura: {}, maxTokens: {}",
                appConfig.getLlm().getModel(),
                appConfig.getLlm().getTemperature(),
                appConfig.getLlm().getMaxTokens());

            ChatResponse response = chatModel.call(
                                    new Prompt(
                                        messages,
                                        AnthropicChatOptions
                                            .builder()
                                            .model(appConfig.getLlm().getModel())
                                            .temperature(appConfig.getLlm().getTemperature())
                                            .build()
                                    ));
            
            

            // Logar consumo de tokens
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                var usage = response.getMetadata().getUsage();
                log.info("Consumo de tokens - Input: {}, Output: {}, Total: {}",
                    usage.getPromptTokens(),
                    usage.getGenerationTokens(),
                    usage.getTotalTokens());
                promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens().longValue() : null;
                generationTokens = usage.getGenerationTokens() != null ? usage.getGenerationTokens().longValue() : null;
                totalTokens = usage.getTotalTokens() != null ? usage.getTotalTokens().longValue() : null;
            }
            
            // Extrair resposta
            String analysis = response.getResults().get(0).getOutput().getContent();
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.info("Processamento LLM concluído em {}ms", processingTime);

            String projectId = projectStructure != null ? projectStructure.getProjectId() : null;
            String projectName = projectStructure != null ? projectStructure.getProjectName() : null;
            String fileId = generateFileId(projectId != null ? projectId : "project");
            
            // Construir resultado
            return ProcessingResult.builder()
                .projectId(projectId)
                .projectName(projectName)
                .fileId(fileId)
                .analysis(analysis)
                .processedAt(LocalDateTime.now())
                .status("COMPLETED")
                .processingTimeMs(processingTime)
                .promptTokens(promptTokens)
                .generationTokens(generationTokens)
                .totalTokens(totalTokens)
                .build();
            
        } catch (Exception e) {
            log.error("Erro ao processar com LLM: {}", e.getMessage(), e);
            
            long processingTime = System.currentTimeMillis() - startTime;

            String projectId = projectStructure != null ? projectStructure.getProjectId() : null;
            String projectName = projectStructure != null ? projectStructure.getProjectName() : null;
            String fileId = generateFileId(projectId != null ? projectId : "project");
            
            return ProcessingResult.builder()
                .projectId(projectId)
                .projectName(projectName)
                .fileId(fileId)
                .analysis("Erro no processamento: " + e.getMessage())
                .processedAt(LocalDateTime.now())
                .status("ERROR")
                .processingTimeMs(processingTime)
                .promptTokens(promptTokens)
                .generationTokens(generationTokens)
                .totalTokens(totalTokens)
                .build();
        }
    }

    private String montarSystemPrompt() {
        var systemPrompt = String.format("%s\n\n%s",
            appConfig.getLlm().getSystemPrompt(),
            appConfig.getLlm().getTemplateHTML());

        log.debug("### System prompt final montado: {}", systemPrompt);
        return systemPrompt;
    }
    

    /**
     * Gera ID único para o arquivo de resultado.
     */
    private String generateFileId(String projectId) {
        return projectId + "_" + System.currentTimeMillis();
    }
}
