package com.processor.kafkallm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processor.kafkallm.config.AppConfig;
import com.processor.kafkallm.model.ProcessingResult;
import com.processor.kafkallm.model.ProjectStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
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
        // log.info("Iniciando processamento LLM para projeto: {}", projectStructure.getProjectId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Preparar o prompt com a estrutura do projeto
            String userPrompt = projectStructure; //buildUserPrompt(projectStructure);
            
            // Criar mensagens
            List<Message> messages = List.of(
                new SystemMessage(appConfig.getLlm().getSystemPrompt()),
                new UserMessage(userPrompt)
            );
            
            ChatResponse response = chatModel.call(
                                    new Prompt(
                                        messages,
                                        AnthropicChatOptions.builder()
                                            .model("claude-3-7-sonnet-latest")
                                            .temperature(0.4)
                                        .build()
                                    ));
            
            // Logar consumo de tokens
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                var usage = response.getMetadata().getUsage();
                log.info("Consumo de tokens - Input: {}, Output: {}, Total: {}",
                    usage.getPromptTokens(),
                    usage.getGenerationTokens(),
                    usage.getTotalTokens());
            }
            
            // Extrair resposta
            String analysis = response.getResults().get(0).getOutput().getContent();
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.info("Processamento LLM concluído em {}ms", processingTime);
            
            // Construir resultado
            return ProcessingResult.builder()
                // .projectId(projectStructure.getProjectId())
                // .projectName(projectStructure.getProjectName())
                // .fileId(generateFileId(projectStructure.getProjectId()))
                .analysis(analysis)
                .processedAt(LocalDateTime.now())
                .status("COMPLETED")
                .processingTimeMs(processingTime)
                .build();
            
        } catch (Exception e) {
            log.error("Erro ao processar com LLM: {}", e.getMessage(), e);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            return ProcessingResult.builder()
                // .projectId(projectStructure.getProjectId())
                // .projectName(projectStructure.getProjectName())
                // .fileId(generateFileId(projectStructure.getProjectId()))
                .analysis("Erro no processamento: " + e.getMessage())
                .processedAt(LocalDateTime.now())
                .status("ERROR")
                .processingTimeMs(processingTime)
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
