package com.processor.kafkallm.service;

import com.processor.kafkallm.config.AppConfig;
import com.processor.kafkallm.model.ProcessingResult;
import com.processor.kafkallm.model.ProjectStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Servico responsavel por extrair zip, mapear projeto e enviar para o LLM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectProcessingService {

    private final AppConfig appConfig;
    private final UploadFileService uploadFileService;
    private final ProjectStructureMapperService mapperService;
    private final LlmProcessingService llmProcessingService;
    private final StorageService storageService;
    private final WebSocketNotificationService notificationService;

    /**
     * Processa um zip previamente salvo e envia a estrutura para o LLM.
     */
    @Async
    public void processUploadedZip(String fileId, String base64Content) {
        notificationService.notifyProcessingStarted(fileId);

        try {
            log.info("### Processando zip: {}", fileId);
            Path zipPath = uploadFileService.saveZipBase64(fileId, base64Content);

            log.info("### Zip salvo em: {}", zipPath);
            Path extractDir = Paths.get(appConfig.getStorage().getBasePath(), "extracted", fileId);
            if (appConfig.getStorage().isCreateDirectories()) {
                Files.createDirectories(extractDir);
            }

            extractZip(zipPath, extractDir);

            log.info("### Zip extraido para: {}", extractDir);
            ProjectStructure structure = mapperService.mapProjectStructure(extractDir);
            structure.setProjectId(fileId);

            // Salvar structure em JSON para debug
            Path debugJsonPath = Paths.get(appConfig.getStorage().getBasePath(), "debug", fileId + "_structure.json");
            log.info("### Salvando estrutura do projeto para debug em: {}", debugJsonPath);
            if (appConfig.getStorage().isCreateDirectories()) {
                Files.createDirectories(debugJsonPath.getParent());
            }
            storageService.saveJson(debugJsonPath, structure);


            // log.info("### Estrutura do projeto mapeada: {}", structure);
            ProcessingResult result = llmProcessingService.processWithLlm(structure);
            String savedPath = storageService.saveHtmlResult(result);
            log.info("### Resultado salvo em: {}", savedPath);

            notificationService.notifyProcessingCompleted(result);
        } catch (Exception e) {
            log.error("### Erro ao processar zip {}: {}", fileId, e.getMessage(), e);
            notificationService.notifyProcessingError(fileId, e.getMessage());
        }
    }

    private void extractZip(Path zipPath, Path targetDir) throws IOException {
        try (InputStream inputStream = Files.newInputStream(zipPath);
             ZipInputStream zipStream = new ZipInputStream(inputStream)) {
            Path normalizedTarget = targetDir.toAbsolutePath().normalize();
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                Path entryPath = Paths.get(entry.getName()).normalize();
                if (entryPath.isAbsolute()) {
                    throw new IOException("### Entrada zip invalida: " + entry.getName());
                }

                Path resolved = normalizedTarget.resolve(entryPath).normalize();
                if (!resolved.startsWith(normalizedTarget)) {
                    throw new IOException("### Entrada zip invalida: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Path parent = resolved.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(zipStream, resolved, StandardCopyOption.REPLACE_EXISTING);
                }
                zipStream.closeEntry();
            }
        }
    }
}
