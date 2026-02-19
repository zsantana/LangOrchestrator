package com.processor.kafkallm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Modelo que representa o resultado do processamento LLM.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingResult {

    private String projectId;
    private String projectName;
    private String fileId;
    private String analysis;
    private LocalDateTime processedAt;
    private String status;
    private String filePath;
    private Long processingTimeMs;
    private Long promptTokens;
    private Long generationTokens;
    private Long totalTokens;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LlmAnalysis {
        private String summary;
        private String mainTechnologies;
        private String projectType;
        private String recommendations;
        private Integer complexityScore;
        private String potentialIssues;
    }
}
