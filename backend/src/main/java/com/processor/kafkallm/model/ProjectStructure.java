package com.processor.kafkallm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Modelo que representa a estrutura de um projeto recebida do Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectStructure {

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty("project_name")
    private String projectName;

    private String timestamp;

    @JsonProperty("root_path")
    private String rootPath;

    private List<FileInfo> files;

    private List<DirectoryInfo> directories;

    private Statistics statistics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileInfo {
        private String name;
        private String path;
        @JsonProperty("full_path")
        private String fullPath;
        private Long size;
        private String extension;
        private String modified;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DirectoryInfo {
        private String name;
        private String path;
        @JsonProperty("full_path")
        private String fullPath;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Statistics {
        @JsonProperty("total_files")
        private Integer totalFiles;

        @JsonProperty("total_directories")
        private Integer totalDirectories;

        @JsonProperty("total_size")
        private Long totalSize;
    }
}
