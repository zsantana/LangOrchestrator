package com.processor.kafkallm.service;

import com.processor.kafkallm.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;

/**
 * Servico responsavel por decodificar e salvar arquivos zip recebidos em base64.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadFileService {

    private final AppConfig appConfig;

    /**
     * Decodifica o base64 e salva como arquivo zip no disco.
     */
    public Path saveZipBase64(String fileId, String base64Content) throws Exception {
        String normalized = normalizeBase64(base64Content);
        byte[] bytes = Base64.getDecoder().decode(normalized);

        Path baseDir = Paths.get(appConfig.getStorage().getBasePath(), "uploads");
        if (appConfig.getStorage().isCreateDirectories()) {
            Files.createDirectories(baseDir);
        }

        Path filePath = baseDir.resolve(fileId + ".zip");
        Files.write(filePath, bytes);

        log.info("Arquivo zip salvo: {}", filePath);
        return filePath;
    }

    /**
     * Recupera o caminho do zip salvo pelo fileId.
     */
    public Optional<Path> getZipPath(String fileId) {
        Path baseDir = Paths.get(appConfig.getStorage().getBasePath(), "uploads");
        Path filePath = baseDir.resolve(fileId + ".zip");

        if (Files.exists(filePath)) {
            return Optional.of(filePath);
        }

        return Optional.empty();
    }

    private String normalizeBase64(String base64Content) {
        if (base64Content == null) {
            return "";
        }

        int commaIndex = base64Content.indexOf(',');
        if (commaIndex >= 0) {
            return base64Content.substring(commaIndex + 1).trim();
        }

        return base64Content.trim();
    }
}
