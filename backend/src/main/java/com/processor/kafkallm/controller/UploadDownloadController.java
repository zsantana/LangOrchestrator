package com.processor.kafkallm.controller;

import com.processor.kafkallm.service.UploadFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controller REST para download de arquivos zip enviados via upload.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UploadDownloadController {

    private final UploadFileService uploadFileService;

    /**
     * Endpoint para download do zip enviado.
     *
     * @param fileId ID do arquivo
     * @return Arquivo zip enviado
     */
    @GetMapping("/uploaded-files/{fileId}/download")
    public ResponseEntity<Resource> downloadUploadedZip(@PathVariable String fileId) {
        log.info("Requisicao de download de upload recebida para fileId: {}", fileId);

        try {
            Path filePath = uploadFileService.getZipPath(fileId)
                .orElseThrow(() -> new RuntimeException("Arquivo nao encontrado: " + fileId));

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error("Arquivo nao encontrado ou nao pode ser lido: {}", fileId);
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/zip";
            }

            log.info("Download iniciado para arquivo: {}", filePath.getFileName());

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                .body(resource);

        } catch (Exception e) {
            log.error("Erro ao processar download: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
