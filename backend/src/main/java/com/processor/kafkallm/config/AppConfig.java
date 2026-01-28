package com.processor.kafkallm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configurações da aplicação.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private LlmConfig llm = new LlmConfig();
    private StorageConfig storage = new StorageConfig();

    @Data
    public static class LlmConfig {
        private String model = "claude-3-5-sonnet-20241022";
        private Integer maxTokens = 8000;
        private Double temperature = 0.7;
        private String systemPrompt = """
                Você é um especialista em análise de código e arquitetura de software.
                Analise a estrutura do projeto fornecida e forneça:
                
                1. **Resumo Geral**: Uma visão geral do projeto
                2. **Tecnologias Principais**: Identifique as principais tecnologias e frameworks
                3. **Tipo de Projeto**: Classifique o tipo de projeto (web, mobile, desktop, etc.)
                4. **Recomendações**: Sugestões de melhorias na estrutura
                5. **Pontuação de Complexidade**: De 1 a 10
                6. **Problemas Potenciais**: Identifique possíveis problemas de arquitetura
                
                Forneça sua análise de forma estruturada e objetiva.
                """;
    }

    @Data
    public static class StorageConfig {
        private String basePath = "./output";
        private String filePattern = "{projectId}_{timestamp}.json";
        private boolean createDirectories = true;
        private boolean prettyPrint = true;
    }
}
