package com.processor.kafkallm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processor.kafkallm.model.RepositoryInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GitService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GitService.class);

    @Value("${git.local-path}")
    private String localPath;

    public Path cloneRepository(String repoUrl) throws GitAPIException {
        try {
            String repoName = repoUrl.substring(repoUrl.lastIndexOf("/") + 1).replace(".git", "");
            var uPath = localPath + "/" + UUID.randomUUID() + "/" + repoName;
            Git.cloneRepository()
               .setURI(repoUrl)
               .setDirectory(new File(uPath))
               .call();
            return Path.of(uPath);
        } catch (GitAPIException e) {
            logger.error("Erro ao clonar repositório: {}", repoUrl, e);
            throw e;
        }
    }

    // Com autenticação
    public Path clonePrivateRepo(String repoUrl, String token) throws GitAPIException {
        try {
            String repoName = repoUrl.substring(repoUrl.lastIndexOf("/") + 1).replace(".git", "");
            var uPath = localPath + "/" + UUID.randomUUID() + "/" + repoName;
            Git.cloneRepository()
               .setURI(repoUrl)
               .setDirectory(new File(uPath))
               .setCredentialsProvider(
                   new UsernamePasswordCredentialsProvider(token, "")
               )
               .call();
            return Path.of(uPath);
        } catch (GitAPIException e) {
            logger.error("Erro ao clonar repositório privado: {}", repoUrl, e);
            throw e;
        }
    }

    // Criar método para listar projetos a partir de uma url base do github, gitlab, etc. (opcional)
    public List<RepositoryInfo> listRepositories(String baseUrl, String token) throws IOException {
        List<RepositoryInfo> repositories = new ArrayList<>();
        String endpoint = resolveRepositoriesEndpoint(baseUrl);

        try {
            URL url = URI.create(endpoint).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github+json");

            if (token != null && !token.isBlank()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }

            int status = connection.getResponseCode();
            BufferedReader reader;
            if (status >= 200 && status < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            if (status < 200 || status >= 300) {
                logger.error("Erro ao listar repositórios. Endpoint: {}, status: {}, body: {}", endpoint, status, response);
                throw new IOException("Falha ao consultar endpoint de repositórios. HTTP " + status);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.toString());
            if (!root.isArray()) {
                logger.error("Resposta inesperada ao listar repositórios. Endpoint: {}, body: {}", endpoint, response);
                throw new IOException("Resposta inválida do endpoint de repositórios");
            }

            for (JsonNode repo : root) {
                if (repo.hasNonNull("clone_url")) {
                    String name = repo.hasNonNull("name") ? repo.get("name").asText() : "";
                    String cloneUrl = repo.get("clone_url").asText();
                    String language = repo.hasNonNull("language") ? repo.get("language").asText() : "UNKNOWN";
                    String projectType = classifyProjectType(language);
                    repositories.add(new RepositoryInfo(name, cloneUrl, language, projectType));
                }
            }
        } catch (IOException e) {
            logger.error("Erro ao listar repositórios para URL base: {}", baseUrl, e);
            throw e;
        }

        return repositories;
    }

    private String resolveRepositoriesEndpoint(String baseUrl) {
        URI uri = URI.create(baseUrl);
        String host = uri.getHost();

        if ("github.com".equalsIgnoreCase(host)) {
            String path = uri.getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                throw new IllegalArgumentException("URL do GitHub inválida para listagem: " + baseUrl);
            }

            String[] segments = path.replaceAll("^/|/$", "").split("/");
            if (segments.length >= 1 && !segments[0].isBlank()) {
                return "https://api.github.com/users/" + segments[0] + "/repos?per_page=100";
            }
        }

        return baseUrl;
    }

    private String classifyProjectType(String language) {
        if (language == null || language.isBlank()) {
            return "UNKNOWN";
        }

        if ("Java".equalsIgnoreCase(language) || "Kotlin".equalsIgnoreCase(language)
                || "Scala".equalsIgnoreCase(language)) {
            return "JAVA";
        }

        if ("Python".equalsIgnoreCase(language)) {
            return "PYTHON";
        }

        if ("JavaScript".equalsIgnoreCase(language) || "TypeScript".equalsIgnoreCase(language)
                || "Node".equalsIgnoreCase(language)) {
            return "NODE";
        }

        return "UNKNOWN";
    }

}