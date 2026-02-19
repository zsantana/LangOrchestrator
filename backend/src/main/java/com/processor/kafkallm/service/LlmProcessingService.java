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

            log.info("### System prompt carregado: {}", appConfig.getLlm().getSystemPrompt());
            List<Message> messages = List.of(
                new SystemMessage(appConfig.getLlm().getSystemPrompt()),
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

    /**
     * Constrói o prompt do usuário com as informações do projeto.
     */
    private String buildUserPrompt(ProjectStructure projectStructure) {
        try {
            // Criar um resumo estruturado da informação
            StringBuilder prompt = new StringBuilder();
            prompt.append("Analise a seguinte estrutura de projeto:\n\n");
            prompt.append("**Projeto:** ").append(projectStructure.getProjectName()).append("\n");
            prompt.append("**ID:** ").append(projectStructure.getProjectId()).append("\n\n");
            
            // Estatísticas
            if (projectStructure.getStatistics() != null) {
                var stats = projectStructure.getStatistics();
                prompt.append("**Estatísticas:**\n");
                prompt.append("- Total de arquivos: ").append(stats.getTotalFiles()).append("\n");
                prompt.append("- Total de diretórios: ").append(stats.getTotalDirectories()).append("\n");
                prompt.append("- Tamanho total: ").append(formatSize(stats.getTotalSize())).append("\n\n");
            }
            
            // Extensões de arquivo (análise de tecnologias)
            prompt.append("**Extensões de arquivo encontradas:**\n");
            var extensions = projectStructure.getFiles().stream()
                .map(ProjectStructure.FileInfo::getExtension)
                .filter(ext -> ext != null && !ext.isEmpty())
                .distinct()
                .toList();
            extensions.forEach(ext -> prompt.append("- ").append(ext).append("\n"));
            
            prompt.append("\n**Estrutura de diretórios:**\n");
            projectStructure.getDirectories().stream()
                .limit(20) // Limitar para não exceder tokens
                .forEach(dir -> prompt.append("- ").append(dir.getPath()).append("\n"));
            
            prompt.append("\n**Principais arquivos:**\n");
            projectStructure.getFiles().stream()
                .limit(30) // Limitar para não exceder tokens
                .forEach(file -> prompt.append("- ")
                    .append(file.getPath())
                    .append(" (")
                    .append(formatSize(file.getSize()))
                    .append(")\n"));
            
            prompt.append("\n\nPor favor, forneça uma análise detalhada em formato JSON.");
            
            return prompt.toString();
            
        } catch (Exception e) {
            log.error("Erro ao construir prompt: {}", e.getMessage());
            return "Estrutura do projeto: " + projectStructure.getProjectName();
        }
    }

    /**
     * Formata tamanho de arquivo para leitura humana.
     */
    private String formatSize(Long size) {
        if (size == null) return "0 B";
        
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    /**
     * Gera ID único para o arquivo de resultado.
     */
    private String generateFileId(String projectId) {
        return projectId + "_" + System.currentTimeMillis();
    }
}
