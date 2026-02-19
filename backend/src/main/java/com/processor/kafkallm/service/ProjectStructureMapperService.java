package com.processor.kafkallm.service;

import com.processor.kafkallm.model.ProjectStructure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Serviço para mapear a estrutura de projetos Spring Boot/Maven.
 */
@Slf4j
@Service
public class ProjectStructureMapperService {

    private static final long MAX_CONTENT_CHARS = 1024L * 1024L;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".java",
        ".xml",
        ".properties",
        ".yaml",
        ".yml"
    );

    private static final Set<String> ALLOWED_FILENAMES = Set.of(
        "pom.xml",
        "application.properties",
        "application.yaml",
        "application.yml",
        "application-dev.properties",
        "application-prod.properties",
        "application-test.properties",
        "application-dev.yaml",
        "application-prod.yaml",
        "application-test.yaml",
        "application-dev.yml",
        "application-prod.yml",
        "application-test.yml"
    );

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
        "target",
        ".mvn",
        "node_modules",
        ".git",
        ".idea",
        ".vscode",
        "__pycache__",
        "build",
        "dist"
    );

    private static final List<Charset> ENCODINGS = List.of(
        StandardCharsets.UTF_8,
        StandardCharsets.ISO_8859_1,
        Charset.forName("windows-1252")
    );

    /**
     * Mapeia a estrutura do projeto, filtrando apenas artefatos Java e Spring Boot/Maven.
     */
    public ProjectStructure mapProjectStructure(Path rootPath) {
        boolean isSpringMaven = isSpringBootMavenProject(rootPath);

        ProjectStructure structure = ProjectStructure.builder()
            .projectName(resolveProjectName(rootPath))
            .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .rootPath(rootPath.toString())
            .isSpringBootMaven(isSpringMaven)
            .files(new ArrayList<>())
            .directories(new ArrayList<>())
            .statistics(ProjectStructure.Statistics.builder()
                .totalFiles(0)
                .totalDirectories(0)
                .totalSize(0L)
                .javaFiles(0)
                .configFiles(0)
                .mavenFiles(0)
                .build())
            .build();

        if (!isSpringMaven) {
            structure.setWarning("Projeto não identificado como Spring Boot/Maven. Nenhum arquivo foi mapeado.");
            return structure;
        }

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(rootPath) && !shouldIncludeDirectory(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    if (!dir.equals(rootPath)) {
                        ProjectStructure.DirectoryInfo dirInfo = ProjectStructure.DirectoryInfo.builder()
                            .name(dir.getFileName().toString())
                            .path(toRelativePath(rootPath, dir))
                            .fullPath(dir.toString())
                            .build();
                        structure.getDirectories().add(dirInfo);
                        structure.getStatistics().setTotalDirectories(
                            structure.getStatistics().getTotalDirectories() + 1
                        );
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!shouldIncludeFile(file)) {
                        return FileVisitResult.CONTINUE;
                    }

                    try {
                        long size = Files.size(file);
                        String extension = getExtension(file);
                        String modified = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(file).toInstant(),
                            ZoneId.systemDefault()
                        ).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                        ProjectStructure.FileInfo fileInfo = ProjectStructure.FileInfo.builder()
                            .name(file.getFileName().toString())
                            .path(toRelativePath(rootPath, file))
                            .fullPath(file.toString())
                            .size(size)
                            .extension(extension)
                            .modified(modified)
                            .type(categorizeFile(file))
                            .content(readFileContent(file))
                            .build();

                        structure.getFiles().add(fileInfo);
                        structure.getStatistics().setTotalFiles(
                            structure.getStatistics().getTotalFiles() + 1
                        );
                        structure.getStatistics().setTotalSize(
                            structure.getStatistics().getTotalSize() + size
                        );

                        if (".java".equalsIgnoreCase(extension)) {
                            structure.getStatistics().setJavaFiles(
                                structure.getStatistics().getJavaFiles() + 1
                            );
                        } else if (".properties".equalsIgnoreCase(extension)
                            || ".yaml".equalsIgnoreCase(extension)
                            || ".yml".equalsIgnoreCase(extension)) {
                            structure.getStatistics().setConfigFiles(
                                structure.getStatistics().getConfigFiles() + 1
                            );
                        } else if (".xml".equalsIgnoreCase(extension)) {
                            structure.getStatistics().setMavenFiles(
                                structure.getStatistics().getMavenFiles() + 1
                            );
                        }
                    } catch (IOException e) {
                        log.debug("Falha ao ler arquivo {}: {}", file, e.getMessage());
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.debug("Falha ao escanear diretório {}: {}", rootPath, e.getMessage());
        }

        return structure;
    }

    private boolean isSpringBootMavenProject(Path rootPath) {
        try (Stream<Path> paths = Files.walk(rootPath)) {
            return paths
                .filter(path -> path.getFileName() != null && "pom.xml".equals(path.getFileName().toString()))
                .anyMatch(this::pomContainsSpringBoot);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean pomContainsSpringBoot(Path pomPath) {
        try {
            String content = Files.readString(pomPath, StandardCharsets.UTF_8).toLowerCase();
            return content.contains("spring-boot") || content.contains("quarkus");
        } catch (IOException e) {
            return false;
        }
    }

    private boolean shouldIncludeFile(Path filePath) {
        String extension = getExtension(filePath).toLowerCase();
        if (ALLOWED_EXTENSIONS.contains(extension)) {
            return true;
        }

        String name = filePath.getFileName().toString();
        return ALLOWED_FILENAMES.contains(name);
    }

    private boolean shouldIncludeDirectory(Path dirPath) {
        String name = dirPath.getFileName() != null ? dirPath.getFileName().toString() : "";
        return !IGNORED_DIRECTORIES.contains(name);
    }

    private String categorizeFile(Path filePath) {
        String name = filePath.getFileName().toString();
        String extension = getExtension(filePath).toLowerCase();

        if (".java".equals(extension)) {
            return "java_source";
        }
        if ("pom.xml".equals(name)) {
            return "maven_config";
        }
        if (name.startsWith("application")) {
            return "spring_config";
        }
        if (".properties".equals(extension) || ".yaml".equals(extension) || ".yml".equals(extension)) {
            return "config";
        }
        if (".xml".equals(extension)) {
            return "xml_config";
        }
        return "other";
    }

    private String readFileContent(Path filePath) {
        long fileSize;
        try {
            fileSize = Files.size(filePath);
        } catch (IOException e) {
            return "[Erro ao ler arquivo: " + e.getMessage() + "]";
        }

        for (Charset charset : ENCODINGS) {
            try {
                String content = readWithCharset(filePath, charset, MAX_CONTENT_CHARS);
                if (fileSize > MAX_CONTENT_CHARS) {
                    return content
                        + "\n\n[... conteúdo truncado - arquivo muito grande ("
                        + fileSize
                        + " bytes)]";
                }
                return content;
            } catch (IOException e) {
                if (e instanceof CharacterCodingException) {
                    continue;
                }
                return "[Erro ao ler arquivo: " + e.getMessage() + "]";
            }
        }

        return "[Não foi possível ler o conteúdo do arquivo - encoding não suportado]";
    }

    private String readWithCharset(Path filePath, Charset charset, long maxChars) throws IOException {
        CharsetDecoder decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

        StringBuilder builder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(filePath), decoder))) {
            char[] buffer = new char[4096];
            long remaining = maxChars;

            while (remaining > 0) {
                int read = reader.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) {
                    break;
                }
                builder.append(buffer, 0, read);
                remaining -= read;
            }
        }

        return builder.toString();
    }

    private String getExtension(Path filePath) {
        String name = filePath.getFileName().toString();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return name.substring(lastDot);
    }

    private String toRelativePath(Path rootPath, Path path) {
        Path relative = rootPath.relativize(path);
        return relative.toString();
    }

    private String resolveProjectName(Path rootPath) {
        Path fileName = rootPath.getFileName();
        if (fileName != null) {
            return fileName.toString();
        }
        return rootPath.toString();
    }
}
