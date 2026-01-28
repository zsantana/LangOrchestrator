package com.processor.kafkallm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.processor.kafkallm.config.AppConfig;
import com.processor.kafkallm.model.ProcessingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço responsável por salvar e gerenciar resultados do processamento LLM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    
    // Cache de arquivos processados (fileId -> filePath)
    private final Map<String, String> fileCache = new HashMap<>();

    /**
     * Salva o resultado do processamento em arquivo.
     */
    public String saveResult(ProcessingResult result) throws IOException {
        log.info("Salvando resultado para projeto: {}", result.getProjectId());
        
        // Criar diretório base se não existir
        Path baseDir = Paths.get(appConfig.getStorage().getBasePath());
        if (appConfig.getStorage().isCreateDirectories()) {
            Files.createDirectories(baseDir);
        }
        
        // Gerar nome do arquivo
        String fileName = generateFileName(result);
        Path filePath = baseDir.resolve(fileName);
        
        // Preparar conteúdo do arquivo
        Map<String, Object> fileContent = new HashMap<>();
        fileContent.put("project_id", result.getProjectId());
        fileContent.put("project_name", result.getProjectName());
        fileContent.put("file_id", result.getFileId());
        fileContent.put("processed_at", result.getProcessedAt().toString());
        fileContent.put("status", result.getStatus());
        fileContent.put("processing_time_ms", result.getProcessingTimeMs());
        fileContent.put("analysis", result.getAnalysis());
        
        // Escrever arquivo JSON formatado
        String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(fileContent);
        
        Files.writeString(
            filePath,
            jsonContent,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
        
        // Atualizar resultado com o caminho do arquivo
        result.setFilePath(filePath.toString());
        
        // Adicionar ao cache
        fileCache.put(result.getFileId(), filePath.toString());
        
        log.info("Resultado salvo com sucesso: {}", filePath);
        
        return filePath.toString();
    }

    /**
     * Recupera o caminho do arquivo pelo fileId.
     */
    public Optional<Path> getFilePath(String fileId) {
        String cachedPath = fileCache.get(fileId);
        
        if (cachedPath != null) {
            Path path = Paths.get(cachedPath);
            if (Files.exists(path)) {
                return Optional.of(path);
            }
        }
        
        // Se não estiver no cache, procurar no diretório
        try {
            Path baseDir = Paths.get(appConfig.getStorage().getBasePath());
            
            if (!Files.exists(baseDir)) {
                return Optional.empty();
            }
            
            return Files.walk(baseDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().contains(fileId))
                .findFirst();
                
        } catch (IOException e) {
            log.error("Erro ao buscar arquivo: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Verifica se um arquivo existe.
     */
    public boolean fileExists(String fileId) {
        return getFilePath(fileId).isPresent();
    }

    /**
     * Remove um arquivo.
     */
    public boolean deleteFile(String fileId) {
        Optional<Path> filePath = getFilePath(fileId);
        
        if (filePath.isPresent()) {
            try {
                Files.deleteIfExists(filePath.get());
                fileCache.remove(fileId);
                log.info("Arquivo removido: {}", fileId);
                return true;
            } catch (IOException e) {
                log.error("Erro ao remover arquivo: {}", e.getMessage());
                return false;
            }
        }
        
        return false;
    }

    /**
     * Lista todos os arquivos processados.
     */
    public Map<String, String> listFiles() {
        return new HashMap<>(fileCache);
    }

    /**
     * Gera nome do arquivo baseado no resultado.
     */
    private String generateFileName(ProcessingResult result) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = result.getProcessedAt().format(formatter);
        return String.format("%s_%s.json", result.getFileId(), timestamp);
    }
}
