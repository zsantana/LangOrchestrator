# Documento de Entendimento e Especificação Técnica

**Projeto:** LangOrchestrator — Agente Multi-Modal de Análise de Projetos  
**Versão:** 1.0.0  
**Data:** 2026-02-24  

---

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Objetivos do Sistema](#2-objetivos-do-sistema)
3. [Arquitetura](#3-arquitetura)
4. [Componentes Principais](#4-componentes-principais)
   - 4.1 [Backend — Spring Boot](#41-backend--spring-boot)
   - 4.2 [Frontend — Streamlit (Python)](#42-frontend--streamlit-python)
   - 4.3 [Mensageria — Apache Kafka](#43-mensageria--apache-kafka)
5. [Modelos de Dados](#5-modelos-de-dados)
6. [Endpoints da API REST](#6-endpoints-da-api-rest)
7. [Comunicação WebSocket](#7-comunicação-websocket)
8. [Fluxo de Processamento](#8-fluxo-de-processamento)
9. [Configuração e Variáveis de Ambiente](#9-configuração-e-variáveis-de-ambiente)
10. [Infraestrutura e Implantação](#10-infraestrutura-e-implantação)
11. [Tecnologias e Dependências](#11-tecnologias-e-dependências)
12. [Segurança](#12-segurança)
13. [Limitações Conhecidas e Decisões de Design](#13-limitações-conhecidas-e-decisões-de-design)

---

## 1. Visão Geral

O **LangOrchestrator** é um sistema de análise automatizada de projetos Java/Spring Boot. O usuário faz o upload de um arquivo ZIP contendo o código-fonte do projeto; o sistema extrai e mapeia a estrutura do projeto, envia o conteúdo para um Large Language Model (LLM) — Anthropic Claude —, e devolve ao usuário uma análise detalhada em formato HTML, acompanhada de notificações em tempo real via WebSocket.

```
┌───────────────┐   POST /api/v1/upload-file   ┌─────────────────────────────────┐
│  Cliente HTTP │ ──────────────────────────── ▶│  Backend (Spring Boot : 8080)    │
│  ou Frontend  │                               │                                  │
│  (Streamlit)  │ ◀──── WebSocket /ws ──────── │  1. Salva ZIP (base64)           │
└───────────────┘   notificações em tempo real  │  2. Extrai ZIP                   │
                                                │  3. Mapeia estrutura do projeto  │
                                                │  4. Envia para Claude (LLM)      │
                                                │  5. Salva resultado em HTML      │
                                                │  6. Notifica via WebSocket       │
                                                └─────────────────────────────────┘
```

---

## 2. Objetivos do Sistema

| # | Objetivo |
|---|----------|
| 1 | Receber projetos Java/Spring Boot em formato ZIP via API REST |
| 2 | Extrair e mapear automaticamente a estrutura de arquivos do projeto |
| 3 | Analisar a estrutura utilizando IA (Anthropic Claude) |
| 4 | Produzir um relatório HTML com análise completa: arquitetura, tecnologias, complexidade, débitos técnicos, diagrama ER |
| 5 | Notificar o cliente em tempo real sobre o estado do processamento via WebSocket (STOMP) |
| 6 | Disponibilizar o resultado para download via API REST |

---

## 3. Arquitetura

O sistema segue uma arquitetura orientada a eventos assíncronos com processamento em segundo plano, composta por:

```
┌────────────────────────────────────────────────────────────────────────┐
│                         Docker Network (kafka-network)                  │
│                                                                          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────────────┐  │
│  │  Zookeeper   │    │    Kafka     │    │  Backend Spring Boot      │  │
│  │  :2181       │◀──▶│  :9092       │    │  :8080                    │  │
│  └──────────────┘    └──────┬───────┘    │  ┌────────────────────┐  │  │
│                             │            │  │ UploadController    │  │  │
│  ┌──────────────┐           │            │  │ ResultDownload      │  │  │
│  │  Kafka UI    │           │            │  │ Controller          │  │  │
│  │  :9000       │◀──────────┘            │  │ WebSocketController │  │  │
│  └──────────────┘                        │  ├────────────────────┤  │  │
│                                          │  │ ProjectProcessing   │  │  │
│  ┌──────────────┐                        │  │ Service (@Async)    │  │  │
│  │  Frontend    │                        │  │ LlmProcessingService│  │  │
│  │  Streamlit   │──REST + WebSocket ────▶│  │ StorageService      │  │  │
│  │  :8501       │                        │  │ WebSocketNotification│  │  │
│  └──────────────┘                        │  └────────────────────┘  │  │
│                                          └──────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Padrões Arquiteturais Utilizados

- **MVC (Model-View-Controller):** Controllers REST e WebSocket com separação de responsabilidades em camadas (controller → service → storage).
- **Processamento Assíncrono:** A análise do projeto é executada em thread separada (`@Async`) para não bloquear a resposta HTTP.
- **Observer / Event-Driven (WebSocket STOMP):** O backend publica notificações em tópicos; os clientes se subscrevem e recebem atualizações em tempo real.
- **Repository Pattern simplificado:** `StorageService` abstrai operações de leitura e escrita no sistema de arquivos.

---

## 4. Componentes Principais

### 4.1 Backend — Spring Boot

**Localização:** `backend/`  
**Porta:** `8080`  
**Linguagem:** Java 21  
**Framework:** Spring Boot 3.3  

#### Estrutura de Pacotes

```
com.processor.kafkallm/
│
├── LlmProcessorApplication.java          # Classe principal (@SpringBootApplication)
│
├── config/
│   ├── AppConfig.java                    # Configurações de LLM e Storage (lê application.yml e system_prompt.md)
│   ├── AsyncConfig.java                  # Configura ThreadPoolTaskExecutor para @Async
│   ├── JacksonConfig.java                # Configura ObjectMapper (datas, módulos)
│   └── WebSocketConfig.java             # Configura endpoints STOMP (/ws, /ws-native)
│
├── controller/
│   ├── UploadController.java            # POST /api/v1/upload-file
│   ├── ResultDownloadController.java    # GET  /api/v1/results/{fileId}/download
│   └── UploadDownloadController.java    # Endpoints auxiliares de upload/download
│
├── websocket/
│   └── WebSocketController.java         # /app/ping → /topic/pong; /app/subscribe → /topic/subscribed
│
├── service/
│   ├── ProjectProcessingService.java    # Orquestra extração, mapeamento e processamento LLM (@Async)
│   ├── ProjectStructureMapperService.java # Mapeia estrutura de arquivos de um diretório extraído
│   ├── LlmProcessingService.java        # Chama Anthropic Claude via Spring AI
│   ├── StorageService.java              # Salva e recupera resultados no sistema de arquivos
│   ├── UploadFileService.java           # Salva o ZIP recebido em base64
│   └── WebSocketNotificationService.java # Envia notificações STOMP aos clientes
│
└── model/
    ├── ProjectStructure.java            # Estrutura mapeada do projeto (arquivos, diretórios, estatísticas)
    ├── ProcessingResult.java            # Resultado do processamento LLM
    └── ProcessingNotification.java      # Payload de notificação WebSocket
```

#### Responsabilidades por Camada

| Camada | Classe | Responsabilidade |
|--------|--------|-----------------|
| Controller | `UploadController` | Recebe requisição de upload (ZIP em base64), delega ao serviço de processamento |
| Controller | `ResultDownloadController` | Serve o arquivo HTML de resultado para download |
| Service | `ProjectProcessingService` | Coordena o pipeline completo de forma assíncrona |
| Service | `ProjectStructureMapperService` | Percorre o diretório extraído e constrói o modelo `ProjectStructure` |
| Service | `LlmProcessingService` | Constrói prompt, chama Claude, retorna `ProcessingResult` |
| Service | `StorageService` | Persiste e recupera resultados em disco (JSON/HTML) |
| Service | `WebSocketNotificationService` | Publica eventos nos tópicos STOMP |
| Config | `AppConfig` | Carrega configurações de LLM e armazenamento; lê `system_prompt.md` |

---

### 4.2 Frontend — Streamlit (Python)

**Localização:** `frontend/` (não incluído nesta versão do repositório)  
**Porta:** `8501`  
**Linguagem:** Python 3.8+  

O frontend provê uma interface web interativa para:
- Upload de arquivo ZIP via drag-and-drop
- Visualização da estrutura de arquivos extraída
- Acompanhamento em tempo real do processamento via WebSocket
- Download do relatório HTML gerado

---

### 4.3 Mensageria — Apache Kafka

**Versão:** Confluent 7.5.0  
**Broker:** `kafka:9092` (interno) / `localhost:9092` (externo)  
**Interface gráfica:** Kafka UI na porta `9000`

O Kafka está presente na infraestrutura como broker de mensagens e é referenciado nas configurações, permitindo a extensão da arquitetura para consumo assíncrono de mensagens (ex: publicação da estrutura de projetos via tópico `project-structure`). Na versão atual, o fluxo principal utiliza a API REST com processamento `@Async` no próprio backend.

---

## 5. Modelos de Dados

### 5.1 `ProjectStructure` — Estrutura do Projeto

Representa a estrutura mapeada do projeto após extração do ZIP.

```json
{
  "project_id": "string",
  "project_name": "string",
  "timestamp": "ISO-8601",
  "root_path": "string",
  "is_spring_boot_maven": true,
  "warning": "string | null",
  "files": [
    {
      "name": "string",
      "path": "string (relativo)",
      "full_path": "string (absoluto)",
      "size": 0,
      "extension": ".java",
      "modified": "ISO-8601",
      "type": "java_source | maven_config | spring_config | config | xml_config | other",
      "content": "string (conteúdo do arquivo)"
    }
  ],
  "directories": [
    {
      "name": "string",
      "path": "string (relativo)",
      "full_path": "string (absoluto)"
    }
  ],
  "statistics": {
    "total_files": 0,
    "total_directories": 0,
    "total_size": 0,
    "java_files": 0,
    "config_files": 0,
    "maven_files": 0
  }
}
```

**Regras de mapeamento:**
- Apenas arquivos com extensões `.java`, `.xml`, `.properties`, `.yaml`, `.yml` são incluídos. Arquivos com outras extensões (`.json`, `.gradle`, `.md`, `.txt`, etc.) são **excluídos** do mapeamento.
- Diretórios ignorados: `target`, `.mvn`, `node_modules`, `.git`, `.idea`, `.vscode`, `__pycache__`, `build`, `dist`.
- Conteúdo de arquivo limitado a **1 MB** por arquivo; arquivos maiores são truncados.
- Somente projetos Spring Boot ou Quarkus (com `pom.xml` contendo `spring-boot` ou `quarkus`) são mapeados. Caso contrário, a estrutura é retornada vazia com campo `warning`.
- Suporte a encodings: UTF-8, ISO-8859-1, Windows-1252.

---

### 5.2 `ProcessingResult` — Resultado do Processamento LLM

```json
{
  "projectId": "string",
  "projectName": "string",
  "fileId": "string",
  "analysis": "string (HTML completo gerado pelo LLM)",
  "processedAt": "ISO-8601",
  "status": "COMPLETED | ERROR",
  "filePath": "string (caminho no disco)",
  "processingTimeMs": 0,
  "promptTokens": 0,
  "generationTokens": 0,
  "totalTokens": 0
}
```

---

### 5.3 `ProcessingNotification` — Notificação WebSocket

```json
{
  "type": "PROCESSING_STARTED | PROCESSING_COMPLETED | PROCESSING_ERROR",
  "projectId": "string",
  "fileId": "string | null",
  "message": "string",
  "timestamp": "ISO-8601",
  "data": {
    "status": "string",
    "processingTimeMs": 0,
    "filePath": "string",
    "downloadUrl": "/api/v1/results/{fileId}/download",
    "error": "string | null",
    "promptTokens": 0,
    "generationTokens": 0,
    "totalTokens": 0
  }
}
```

---

### 5.4 Payload de Upload (Entrada da API)

```json
{
  "id_key_processor": "string (opcional — UUID gerado automaticamente se ausente)",
  "content_file": "string (conteúdo do ZIP em base64)",
  "optional": "string (campo opcional livre)"
}
```

---

## 6. Endpoints da API REST

Base URL: `http://localhost:8080`

| Método | Caminho | Descrição | Request Body | Response |
|--------|---------|-----------|-------------|----------|
| `POST` | `/api/v1/upload-file` | Recebe ZIP em base64 e inicia processamento assíncrono | `UploadRequest` (JSON) | `204 No Content` ou `500` em caso de falha |
| `GET` | `/api/v1/results/{fileId}/download` | Download do arquivo HTML de resultado | — | `200 OK` com `Content-Type: text/html` e header `Content-Disposition: attachment` |

### Exemplo — Upload

```bash
curl -X POST http://localhost:8080/api/v1/upload-file \
  -H "Content-Type: application/json" \
  -d '{
    "id_key_processor": "meu-projeto-123",
    "content_file": "<BASE64_DO_ZIP>"
  }'
```

### Exemplo — Download

```bash
curl -O -J http://localhost:8080/api/v1/results/meu-projeto-123_1700000000000/download
```

### Documentação OpenAPI / Swagger

Disponível em: `http://localhost:8080/swagger-ui/index.html` (via `springdoc-openapi` 2.0.2)

---

## 7. Comunicação WebSocket

O backend implementa WebSocket com protocolo STOMP sobre SockJS.

### Endpoints de Conexão

| Endpoint | Protocolo | Descrição |
|----------|-----------|-----------|
| `ws://localhost:8080/ws` | STOMP + SockJS | Para clientes com fallback HTTP (navegadores) |
| `ws://localhost:8080/ws-native` | STOMP nativo | Para clientes WebSocket nativos (Python, etc.) |

### Tópicos de Subscrição (Servidor → Cliente)

| Tópico | Descrição |
|--------|-----------|
| `/topic/notifications` | Todas as notificações de processamento |
| `/topic/project/{projectId}` | Notificações específicas de um projeto |
| `/topic/pong` | Resposta ao ping de teste |
| `/topic/subscribed` | Confirmação de subscrição |

### Destinos de Envio (Cliente → Servidor)

| Destino | Descrição |
|---------|-----------|
| `/app/ping` | Teste de conectividade (responde em `/topic/pong`) |
| `/app/subscribe` | Registro de cliente (responde em `/topic/subscribed`) |

### Exemplo — Conexão via JavaScript (SockJS + STOMP)

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    stompClient.subscribe('/topic/notifications', (message) => {
        const notification = JSON.parse(message.body);
        console.log('Tipo:', notification.type);
        console.log('Projeto:', notification.projectId);
        if (notification.data?.downloadUrl) {
            console.log('Download:', notification.data.downloadUrl);
        }
    });
});
```

---

## 8. Fluxo de Processamento

### Diagrama de Sequência

```
Cliente          UploadController    ProjectProcessingService   LlmProcessingService   StorageService   WebSocket
   │                    │                      │                        │                    │              │
   │──POST /upload-file─▶│                      │                        │                    │              │
   │                    │──processUploadedZip()─▶│                        │                    │              │
   │◀───204 No Content──│  (assíncrono/@Async)  │                        │                    │              │
   │                    │                      │──notifyStarted()───────────────────────────────────────────▶│
   │                    │                      │                        │                    │              │
   │                    │                      │  1. saveZipBase64()    │                    │              │
   │                    │                      │  2. extractZip()       │                    │              │
   │                    │                      │  3. mapProjectStructure│                    │              │
   │                    │                      │  4. saveJson(debug)    │                    │              │
   │                    │                      │──processWithLlm()──────▶│                    │              │
   │                    │                      │                        │──Claude API call───▶              │
   │                    │                      │                        │◀──HTML analysis────               │
   │                    │                      │◀──ProcessingResult─────│                    │              │
   │                    │                      │──saveHtmlResult()──────────────────────────▶│              │
   │                    │                      │◀──filePath─────────────────────────────────│              │
   │                    │                      │──notifyCompleted()──────────────────────────────────────────▶│
   │◀───WebSocket msg───────────────────────────────────────────────────────────────────────────────────────│
   │                    │                      │                        │                    │              │
   │──GET /results/{id}/download───────────────────────────────────────────────────────────▶│              │
   │◀──HTML file────────────────────────────────────────────────────────────────────────────│              │
```

### Etapas Detalhadas

1. **Recepção do Upload** (`UploadController.uploadFile`):
   - Recebe JSON com `id_key_processor` e `content_file` (ZIP em base64).
   - Gera UUID se `id_key_processor` não for fornecido.
   - Retorna `204 No Content` imediatamente; processamento ocorre em segundo plano.

2. **Processamento Assíncrono** (`ProjectProcessingService.processUploadedZip`, anotado com `@Async`):
   - Decodifica base64 e salva o ZIP em disco.
   - Extrai o ZIP protegendo contra ataques de *zip slip* (valida que entradas não escapam do diretório alvo).
   - Chama `ProjectStructureMapperService` para construir o modelo `ProjectStructure`.
   - Salva a estrutura em JSON de debug (`output/debug/{fileId}_structure.json`).

3. **Análise pelo LLM** (`LlmProcessingService.processWithLlm`):
   - Serializa `ProjectStructure` em JSON como prompt de usuário.
   - Combina com o *system prompt* carregado de `classpath:system_prompt.md`.
   - Invoca `AnthropicChatModel` via Spring AI com as opções configuradas (modelo, temperatura).
   - Registra consumo de tokens (input, output, total).
   - Retorna `ProcessingResult` com o HTML gerado e metadados.

4. **Persistência** (`StorageService.saveHtmlResult`):
   - Salva o conteúdo HTML em `output/{fileId}_{timestamp}.html`.
   - Mantém cache em memória `Map<fileId, filePath>` para lookups rápidos.

5. **Notificação** (`WebSocketNotificationService`):
   - Publica `ProcessingNotification` com `type=PROCESSING_COMPLETED` em `/topic/notifications` e `/topic/project/{projectId}`.
   - Notificação inclui `downloadUrl` para que o cliente possa baixar o resultado.

---

## 9. Configuração e Variáveis de Ambiente

### Variáveis de Ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `ANTHROPIC_API_KEY` | — | **Obrigatória.** Chave de API da Anthropic |
| `ANTHROPIC_MODEL` | `claude-3-5-sonnet-20241022` | Modelo Claude a utilizar |
| `ANTHROPIC_MAX_TOKENS` | `20000` | Número máximo de tokens na resposta do LLM (lido diretamente em `AppConfig.LlmConfig` via `System.getenv`) |

### `application.yml` — Configurações Principais

```yaml
spring:
  application:
    name: kafka-llm-processor
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: kafka-llm-processor-group
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat.options:
        model: ${ANTHROPIC_MODEL:claude-3-5-sonnet-20241022}
        max-tokens: 20000
        temperature: 0.7
  servlet.multipart:
    max-file-size: 100MB
    max-request-size: 100MB
  jackson.stream-read-constraints:
    max-string-length: 900000000   # 900.000.000 caracteres (~900 MB em SI / ~858 MiB) — suporta projetos com arquivos grandes

app:
  llm:
    model: ${ANTHROPIC_MODEL:claude-3-5-sonnet-20241022}
    max-tokens: 20000
    temperature: 0.7
    system-prompt-path: classpath:system_prompt.md
  storage:
    base-path: ./output
    file-pattern: "{projectId}_{timestamp}.json"
    create-directories: true
    pretty-print: true

server:
  port: 8080
```

### System Prompt (`system_prompt.md`)

O *system prompt* instrui o LLM a produzir uma análise completa em **HTML**, incluindo:

1. Resumo Geral do projeto
2. Stack Tecnológico (frameworks, Java version, Maven/Gradle)
3. Padrão Arquitetural
4. Estrutura de Módulos e pacotes
5. Dependências críticas
6. Padrões de Design identificados
7. Pontuação de Complexidade (1–10)
8. Problemas Potenciais (débitos técnicos, violações SOLID, riscos de segurança)
9. Recomendações de Melhoria
10. Resumo Executivo com próximos passos
11. Entidades e Relacionamentos
12. Diagrama ER em Mermaid (formato `erDiagram`)

O HTML produzido usa tags semânticas HTML5, CSS inline, tipografia e o script Mermaid.js via CDN para renderizar o diagrama diretamente no navegador.

---

## 10. Infraestrutura e Implantação

### Docker Compose — Serviços

| Serviço | Imagem | Porta Exposta | Dependência |
|---------|--------|--------------|-------------|
| `zookeeper` | `confluentinc/cp-zookeeper:7.5.0` | `2181` | — |
| `kafka` | `confluentinc/cp-kafka:7.5.0` | `9092`, `9101` (JMX) | `zookeeper` |
| `kafka-ui` | `provectuslabs/kafka-ui:latest` | `9000` | `kafka` |

> O serviço `backend` pode ser executado localmente ou adicionado ao Compose usando o `Dockerfile` em `backend/`.

### Dockerfile (`backend/Dockerfile`)

Build multi-stage (Maven + JRE 21) produzindo imagem otimizada para produção.

### Scripts de Operação

| Script | Ação |
|--------|------|
| `./start.sh` | Inicia todos os serviços via Docker Compose |
| `./stop.sh` | Para e remove todos os containers |

### Pré-requisitos

- **Docker** e **Docker Compose** instalados
- **Java 21** (execução local do backend)
- **Python 3.8+** (execução local do frontend)
- Variável `ANTHROPIC_API_KEY` configurada

### Procedimento de Inicialização

```bash
# 1. Configurar a chave de API
export ANTHROPIC_API_KEY=sua_chave_api

# 2. Iniciar infraestrutura (Kafka + Zookeeper + Kafka UI)
./start.sh

# 3. Iniciar o backend localmente
cd backend
mvn spring-boot:run

# 4. (Opcional) Iniciar o frontend
cd frontend
pip install -r requirements.txt
streamlit run app.py
```

---

## 11. Tecnologias e Dependências

### Backend

| Dependência | Versão | Finalidade |
|-------------|--------|-----------|
| Spring Boot | 3.3.0 | Framework principal |
| Spring AI (Anthropic) | 1.0.0-M5 | Integração com Claude LLM |
| Spring WebSocket (STOMP) | (Boot managed) | Notificações em tempo real |
| Spring Kafka | (Boot managed) | Integração com Apache Kafka |
| Lombok | (Boot managed) | Redução de boilerplate |
| Jackson Databind + JSR310 | (Boot managed) | Serialização JSON |
| SpringDoc OpenAPI | 2.0.2 | Documentação Swagger UI |
| Java | 21 | Linguagem |
| Maven | 3.9+ | Build e gerenciamento de dependências |

### Frontend

| Dependência | Finalidade |
|-------------|-----------|
| Streamlit | Interface web interativa |
| kafka-python | Produtor Kafka |
| websocket-client | Cliente WebSocket nativo |
| requests | Cliente HTTP para downloads |

### Infraestrutura

| Tecnologia | Versão | Finalidade |
|------------|--------|-----------|
| Apache Kafka | 7.5.0 (Confluent) | Broker de mensagens |
| Apache Zookeeper | 7.5.0 (Confluent) | Coordenação do Kafka |
| Kafka UI | latest | Monitoramento visual do Kafka |
| Docker / Docker Compose | — | Containerização |

---

## 12. Segurança

### Proteções Implementadas

| Risco | Mitigação |
|-------|-----------|
| **Zip Slip** | `ProjectProcessingService.extractZip` valida que cada entrada extraída não escapa do diretório alvo usando normalização de caminhos (`normalize()` + `startsWith(targetDir)`) |
| **Payload muito grande** | Limite de 100 MB configurado via `spring.servlet.multipart` |
| **Conteúdo de arquivo muito grande** | Truncamento de conteúdo ao atingir 1 MB por arquivo em `ProjectStructureMapperService` |
| **CORS** | Ambos os controllers REST utilizam `@CrossOrigin(origins = "*")` — adequado para desenvolvimento; deve ser restringido em produção |
| **Chave de API exposta** | `ANTHROPIC_API_KEY` é injetada via variável de ambiente; nunca hardcoded |

### Recomendações de Segurança para Produção

- Restringir `@CrossOrigin` para origens específicas.
- Utilizar HTTPS com TLS para os endpoints REST e WebSocket.
- Adicionar autenticação (e.g., API Key ou OAuth2) ao endpoint `/api/v1/upload-file`.
- Limitar o tamanho e o conteúdo dos projetos aceitos (tipos de arquivo, profundidade de diretórios).

---

## 13. Limitações Conhecidas e Decisões de Design

| # | Limitação / Decisão | Justificativa |
|---|---------------------|--------------|
| 1 | **Somente projetos Spring Boot/Quarkus com Maven são analisados.** O mapeador verifica a presença de `pom.xml` com referências a `spring-boot` ou `quarkus`. Projetos sem esse critério retornam estrutura vazia com aviso. | Foco no domínio Java/Spring Boot; reduz ruído no prompt enviado ao LLM |
| 2 | **Cache de arquivos em memória.** `StorageService` mantém um `HashMap` de `fileId → filePath`. Ao reiniciar a aplicação, o cache é perdido; buscas recorrem ao sistema de arquivos. | Simplicidade; adequado para volume moderado de requisições |
| 3 | **Resultado armazenado em disco local.** Os HTMLs são salvos em `./output/` dentro do container/processo. Em ambientes distribuídos, deve-se substituir por um storage externo (S3, GCS, etc.). | Simplicidade de implementação |
| 4 | **`jackson.stream-read-constraints.max-string-length` configurado para 900 MB.** Necessário para suportar o envio de projetos grandes como JSON string. | Projetos Java podem ter conteúdo de arquivo extenso |
| 5 | **Frontend não está no repositório atual.** O `README.md` referencia um diretório `frontend/` com código Streamlit que não foi incluído nesta versão do repositório. | Repositório focado no backend e infraestrutura |
| 6 | **Processamento síncrono do LLM.** A chamada ao Claude bloqueia a thread do pool `@Async` até receber resposta completa. Para projetos muito grandes, pode atingir o timeout do modelo. | Simplificidade; streaming pode ser implementado em versões futuras |
