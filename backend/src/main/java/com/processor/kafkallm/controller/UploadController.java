package com.processor.kafkallm.controller;

import com.processor.kafkallm.service.ProjectProcessingService;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller REST para upload de arquivo zip em base64.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UploadController {

    private final ProjectProcessingService projectProcessingService;

    /**
     * Endpoint para receber arquivo zip em base64.
     */
    @PostMapping("/upload-file")
    public ResponseEntity<?> uploadFile(@RequestBody UploadRequest request) {

        log.info("### Recebendo payload {}", request);
        String fileId = request.getIdKeyProcessor();
        if (fileId == null || fileId.isBlank()) {
            fileId = UUID.randomUUID().toString();
        }

        log.info("Requisicao de upload recebida. id_key_processor: {}", fileId);

        try {
            projectProcessingService.processUploadedZip(fileId, request.getContentFile());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao salvar arquivo de upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "id_key_processor", fileId,
                "error", "Falha ao processar upload"
            ));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class UploadRequest {
        @JsonProperty("id_key_processor")
        private String idKeyProcessor;

        @JsonProperty("content_file")
        private String contentFile;

        @JsonProperty("optional")
        private String optional;


    }
}
