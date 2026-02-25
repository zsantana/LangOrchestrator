package com.processor.kafkallm.model;

public record RepositoryInfo(
        String name,
        String cloneUrl,
        String language,
        String projectType) {
}
