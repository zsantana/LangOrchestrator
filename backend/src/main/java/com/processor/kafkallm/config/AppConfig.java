package com.processor.kafkallm.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.Data;
// import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

/**
 * Configurações da aplicação.
 */
@Data
@Configuration
// @ConfigurationProperties(prefix = "app")
public class AppConfig {

    private LlmConfig llm = new LlmConfig();
    private StorageConfig storage = new StorageConfig();
    private final ResourceLoader resourceLoader;

    public AppConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void loadSystemPrompt() {
        if (llm.systemPrompt != null && !llm.systemPrompt.isBlank()) {
            return;
        }

        if (llm.systemPromptPath == null || llm.systemPromptPath.isBlank()) {
            return;
        }

        Resource resource = resourceLoader.getResource(llm.systemPromptPath);
        try (var inputStream = resource.getInputStream()) {
            llm.systemPrompt = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Nao foi possivel carregar system_prompt", ex);
        }

        // carregar template HTML
        Resource templateResource = resourceLoader.getResource(llm.templateHTML);
        try (var inputStream = templateResource.getInputStream()) {
            llm.templateHTML = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Nao foi possivel carregar template HTML", ex);
        }
    }

    @Data
    public static class LlmConfig {
        private String model = System.getenv().getOrDefault("ANTHROPIC_MODEL", "claude-3-5-sonnet-20241022");
        private Integer maxTokens = System.getenv().getOrDefault("ANTHROPIC_MAX_TOKENS", "20000").isBlank() ? 20000 : Integer.parseInt(System.getenv().getOrDefault("ANTHROPIC_MAX_TOKENS", "20000"));
        private Double temperature = 0.;
        private String systemPromptPath = "classpath:system_prompt_v3.md";
        private String systemPrompt = "";
        private String templateHTML = "classpath:template.html";
    }

    @Data
    public static class StorageConfig {
        private String basePath = "./output";
        private String filePattern = "{projectId}_{timestamp}.json";
        private boolean createDirectories = true;
        private boolean prettyPrint = true;
    }
}
