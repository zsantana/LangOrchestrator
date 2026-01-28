package com.processor.kafkallm.controller;

import com.processor.kafkallm.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Controller REST para download de arquivos processados.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DownloadController {

    private final StorageService storageService;

    /**
     * Endpoint para download de arquivo processado.
     * 
     * @param fileId ID do arquivo
     * @return Arquivo JSON com resultado do processamento
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        log.info("Requisição de download recebida para fileId: {}", fileId);
        
        try {
            // Buscar caminho do arquivo
            Path filePath = storageService.getFilePath(fileId)
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado: " + fileId));
            
            // Criar recurso
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.error("Arquivo não encontrado ou não pode ser lido: {}", fileId);
                return ResponseEntity.notFound().build();
            }
            
            // Determinar tipo de conteúdo
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_JSON_VALUE;
            }
            
            log.info("Download iniciado para arquivo: {}", filePath.getFileName());
            
            // Retornar arquivo
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

    /**
     * Endpoint para verificar se arquivo existe.
     * 
     * @param fileId ID do arquivo
     * @return Status da existência do arquivo
     */
    @GetMapping("/files/{fileId}/exists")
    public ResponseEntity<Map<String, Object>> checkFileExists(@PathVariable String fileId) {
        boolean exists = storageService.fileExists(fileId);
        
        return ResponseEntity.ok(Map.of(
            "file_id", fileId,
            "exists", exists
        ));
    }

    /**
     * Lista todos os arquivos processados disponíveis.
     * 
     * @return Lista de arquivos
     */
    @GetMapping("/files")
    public ResponseEntity<Map<String, Object>> listFiles() {
        Map<String, String> files = storageService.listFiles();
        
        return ResponseEntity.ok(Map.of(
            "total", files.size(),
            "files", files
        ));
    }

    /**
     * Remove um arquivo processado.
     * 
     * @param fileId ID do arquivo
     * @return Status da remoção
     */
    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String fileId) {
        boolean deleted = storageService.deleteFile(fileId);
        
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                "message", "Arquivo removido com sucesso",
                "file_id", fileId
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "error", "Arquivo não encontrado",
                    "file_id", fileId
                ));
        }
    }

    /**
     * Health check da API.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Download API",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
