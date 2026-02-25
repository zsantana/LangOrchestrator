package com.processor.kafkallm.controller;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.processor.kafkallm.model.RepositoryInfo;
import com.processor.kafkallm.service.GitService;
import com.processor.kafkallm.service.ProjectProcessingService;

@RestController
@RequestMapping("/api/git")
public class GitController {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(GitController.class);

    private final GitService gitService;
    private final ProjectProcessingService projectProcessingService;

    public GitController(GitService gitService, ProjectProcessingService projectProcessingService) {
        this.gitService = gitService;
        this.projectProcessingService = projectProcessingService;
    }

    @PostMapping("/clone/public")
    public ResponseEntity<String> clonePublicRepo(@RequestBody CloneRequest request) throws IOException {
        try {
            var diretory = gitService.cloneRepository(request.repoUrl());
            projectProcessingService.persistProjectData(request.idKeyProcessor(), diretory);
            logger.info("Repositório público clonado com sucesso: {}", request.repoUrl());
            return ResponseEntity.ok("Repositório público clonado com sucesso!");
        } catch (GitAPIException e) {
            logger.error("Erro ao clonar repositório público: {}", request.repoUrl(), e);
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    @PostMapping("/clone/private")
    public ResponseEntity<String> clonePrivateRepo(@RequestBody CloneRequest request) throws IOException {
        try {
            var diretory = gitService.clonePrivateRepo(request.repoUrl(), request.token());
            projectProcessingService.persistProjectData(request.idKeyProcessor(), diretory);    
            logger.info("Repositório privado clonado com sucesso: {}", request.repoUrl());
            return ResponseEntity.ok("Repositório privado clonado com sucesso!");
        } catch (GitAPIException e) {
            logger.error("Erro ao clonar repositório privado: {}", request.repoUrl(), e);
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    @PostMapping("/list")
    public ResponseEntity<List<RepositoryInfo>> listRepositories(@RequestBody ListRepositoriesRequest request) {
        try {
            List<RepositoryInfo> repositories = gitService.listRepositories(request.baseUrl(), request.token());
            logger.info("Listagem de repositórios realizada com sucesso para: {}", request.baseUrl());
            return ResponseEntity.ok(repositories);
        } catch (IOException e) {
            logger.error("Erro ao listar repositórios para URL base: {}", request.baseUrl(), e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    
}
