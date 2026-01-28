# 📂 Guia de Navegação - Fontes Java

## 📍 Localização dos Arquivos

Todos os arquivos fonte Java estão em:
```
kafka-llm-processor/src/main/java/com/processor/kafkallm/
```

## 🗂️ Estrutura Completa do Projeto

```
kafka-llm-processor/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── processor/
│       │           └── kafkallm/
│       │               │
│       │               ├── KafkaLlmProcessorApplication.java  ⭐ MAIN
│       │               │
│       │               ├── config/                    📋 Configurações
│       │               │   ├── AppConfig.java
│       │               │   ├── KafkaConsumerConfig.java
│       │               │   └── WebSocketConfig.java
│       │               │
│       │               ├── model/                     📦 Modelos de Dados
│       │               │   ├── ProjectStructure.java
│       │               │   ├── ProcessingResult.java
│       │               │   └── ProcessingNotification.java
│       │               │
│       │               ├── service/                   🔧 Serviços
│       │               │   ├── LlmProcessingService.java
│       │               │   ├── StorageService.java
│       │               │   └── WebSocketNotificationService.java
│       │               │
│       │               ├── kafka/                     📨 Consumer Kafka
│       │               │   └── ProjectStructureConsumer.java
│       │               │
│       │               ├── controller/                🌐 REST API
│       │               │   └── DownloadController.java
│       │               │
│       │               └── websocket/                 🔌 WebSocket
│       │                   └── WebSocketController.java
│       │
│       └── resources/
│           └── application.yml                        ⚙️ Configurações
│
├── pom.xml                                           📦 Dependências Maven
├── Dockerfile                                        🐳 Container
├── docker-compose.yml                                🐳 Stack completa
└── README.md                                         📖 Documentação
```

## 🎯 Arquivos Principais por Funcionalidade

### 1️⃣ **Consumidor Kafka** (Ponto de Entrada)

**📄 ProjectStructureConsumer.java**
- 📍 Localização: `src/main/java/com/processor/kafkallm/kafka/`
- 🎯 Função: Consome mensagens do tópico `project-structure`
- 🔑 Anotação chave: `@KafkaListener`
- 📝 Responsabilidades:
  - Recebe mensagens do Kafka
  - Deserializa JSON para objeto Java
  - Orquestra o fluxo de processamento
  - Trata erros e exceções

**Fluxo:**
```
Kafka → ProjectStructureConsumer → LlmProcessingService → StorageService → WebSocketNotificationService
```

---

### 2️⃣ **Processamento com LLM**

**📄 LlmProcessingService.java**
- 📍 Localização: `src/main/java/com/processor/kafkallm/service/`
- 🎯 Função: Integração com Anthropic Claude API
- 🤖 Responsabilidades:
  - Constrói prompt para o LLM
  - Faz chamada à API Anthropic
  - Processa resposta do modelo
  - Gera resultado estruturado

**Dependência Spring AI:**
```java
private final AnthropicChatModel chatModel;
```

---

### 3️⃣ **Armazenamento de Resultados**

**📄 StorageService.java**
- 📍 Localização: `src/main/java/com/processor/kafkallm/service/`
- 🎯 Função: Gerencia arquivos de resultado
- 💾 Responsabilidades:
  - Salva resultados em JSON
  - Mantém cache de arquivos
  - Gerencia diretórios
  - Fornece acesso aos arquivos

---

### 4️⃣ **Notificações WebSocket**

**📄 WebSocketNotificationService.java**
- 📍 Localização: `src/main/java/com/processor/kafkallm/service/`
- 🎯 Função: Envia notificações em tempo real
- 🔔 Responsabilidades:
  - Notifica início de processamento
  - Notifica conclusão com sucesso
  - Notifica erros
  - Publica em tópicos WebSocket

**Configuração:**
```java
// WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer
```

---

### 5️⃣ **API REST para Download**

**📄 DownloadController.java**
- 📍 Localização: `src/main/java/com/processor/kafkallm/controller/`
- 🎯 Função: Endpoints REST para download
- 🌐 Endpoints:
  - `GET /api/download/{fileId}` - Download
  - `GET /api/files` - Listar arquivos
  - `GET /api/health` - Health check
  - `DELETE /api/files/{fileId}` - Remover

---

## 🔍 Modelos de Dados

### ProjectStructure.java
- Representa estrutura do projeto recebida do Kafka
- Inclui: files, directories, statistics
- Usa Jackson para deserialização

### ProcessingResult.java
- Resultado do processamento LLM
- Inclui: analysis, status, timing

### ProcessingNotification.java
- Notificação enviada via WebSocket
- Tipos: STARTED, COMPLETED, ERROR

---

## ⚙️ Configurações

### KafkaConsumerConfig.java
- Configura consumer Kafka
- Bootstrap servers
- Deserializers
- Concorrência (3 threads)

### WebSocketConfig.java
- Endpoints WebSocket: `/ws` e `/ws-native`
- Brokers: `/topic` e `/queue`
- CORS configurado

### AppConfig.java
- Propriedades customizadas
- Storage paths
- LLM settings (prompt, tokens, temperature)

---

## 🚀 Como Navegar no Código

### 1. Começe pela Main Class:
```
KafkaLlmProcessorApplication.java
```

### 2. Entenda o fluxo do Consumer:
```
kafka/ProjectStructureConsumer.java
↓
@KafkaListener → consumeProjectStructure()
```

### 3. Veja o processamento LLM:
```
service/LlmProcessingService.java
↓
processWithLlm() → buildUserPrompt() → chatModel.call()
```

### 4. Confira o armazenamento:
```
service/StorageService.java
↓
saveResult() → Files.writeString()
```

### 5. Entenda as notificações:
```
service/WebSocketNotificationService.java
↓
notifyProcessingCompleted() → messagingTemplate.convertAndSend()
```

### 6. Explore a API REST:
```
controller/DownloadController.java
↓
@GetMapping("/api/download/{fileId}")
```

---

## 🛠️ Principais Dependências (pom.xml)

```xml
<!-- Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Spring AI - Anthropic -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>

<!-- WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```

---

## 📝 Anotações Importantes

### Spring Framework
- `@SpringBootApplication` - Main class
- `@Configuration` - Classes de configuração
- `@Component` / `@Service` - Beans gerenciados
- `@RestController` - Controller REST
- `@KafkaListener` - Listener Kafka
- `@MessageMapping` - WebSocket mapping

### Lombok
- `@Data` - Getters/Setters/toString
- `@Builder` - Builder pattern
- `@RequiredArgsConstructor` - Constructor injection
- `@Slf4j` - Logger

### Validação
- `@JsonProperty` - Mapeamento JSON
- `@JsonIgnoreProperties` - Ignorar propriedades

---

## 🔧 Como Modificar o Comportamento

### Alterar Prompt do LLM
```
📄 application.yml
↓
app.llm.system-prompt
```

### Ajustar Concorrência Kafka
```
📄 KafkaConsumerConfig.java
↓
factory.setConcurrency(3); // Número de threads
```

### Customizar Notificações
```
📄 WebSocketNotificationService.java
↓
Adicionar novos métodos notify*()
```

### Adicionar Novos Endpoints
```
📄 DownloadController.java
↓
@GetMapping("/api/seu-endpoint")
```

---

## 🐛 Debug

### Ver logs do Consumer:
```java
// ProjectStructureConsumer.java
log.info("Mensagem recebida - Topic: {}, Partition: {}, Offset: {}", ...);
```

### Ver logs do LLM:
```java
// LlmProcessingService.java
log.info("Iniciando processamento LLM para projeto: {}", ...);
```

### Ver logs do WebSocket:
```java
// WebSocketNotificationService.java
log.info("Enviando notificação de conclusão: {}", ...);
```

---

## 📚 Recursos para Aprender Mais

1. **Spring Kafka**: https://docs.spring.io/spring-kafka/reference/
2. **Spring AI**: https://docs.spring.io/spring-ai/reference/
3. **Spring WebSocket**: https://docs.spring.io/spring-framework/reference/web/websocket.html
4. **Anthropic API**: https://docs.anthropic.com/

---

## ✅ Checklist de Desenvolvimento

Ao modificar o código, verifique:

- [ ] Logs adequados em todos os métodos importantes
- [ ] Tratamento de exceções
- [ ] Validação de entrada
- [ ] Testes unitários (se aplicável)
- [ ] Documentação JavaDoc
- [ ] Configurações em application.yml
- [ ] Atualizar README.md se necessário

---

**🎉 Agora você sabe exatamente onde encontrar cada arquivo fonte do projeto Java!**
