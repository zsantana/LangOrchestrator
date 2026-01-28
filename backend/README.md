# Kafka LLM Processor

Processador de estruturas de projetos usando Kafka + Anthropic Claude LLM.

## 🚀 Funcionalidades

- **Consumidor Kafka**: Consome mensagens do tópico `project-structure`
- **Processamento LLM**: Análise de estrutura de projetos com Anthropic Claude
- **Salvamento de Resultados**: Armazena análises em arquivos JSON
- **Notificações WebSocket**: Notificações em tempo real do processamento
- **API REST**: Download de arquivos processados

## 📋 Requisitos

- Java 21
- Maven 3.9+
- Docker e Docker Compose (opcional)
- Chave API Anthropic Claude

## 🛠️ Estrutura do Projeto

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
│       │               └── controller/                🌐 REST API
│       │                   └── DownloadController.java
│       │
│       └── resources/
│           └── application.yml                        ⚙️ Configurações
│
├── pom.xml                                           📦 Dependências Maven
├── Dockerfile                                        🐳 Container
├── docker-compose.yml                                🐳 Stack completa
└── README.md                                         📖 Documentação
```

## ⚙️ Configuração

### 1. Configurar Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto:

```env
ANTHROPIC_API_KEY=sua_chave_api_aqui
```

### 2. Configurar application.yml

O arquivo `src/main/resources/application.yml` já está configurado com valores padrão.

## 🚀 Executando a Aplicação

### Opção 1: Com Docker Compose (Recomendado)

```bash
# Iniciar toda a stack (Zookeeper + Kafka + Aplicação)
docker-compose up -d

# Ver logs
docker-compose logs -f kafka-llm-processor

# Parar
docker-compose down
```

### Opção 2: Localmente

#### 1. Iniciar Kafka Local

```bash
# Iniciar apenas Kafka e Zookeeper
docker-compose up -d zookeeper kafka
```

#### 2. Executar Aplicação

```bash
# Compilar
mvn clean package

# Executar
export ANTHROPIC_API_KEY=sua_chave_api
mvn spring-boot:run
```

Ou executar o JAR diretamente:

```bash
java -jar target/kafka-llm-processor-1.0.0.jar
```

## 📡 Endpoints

### REST API

- **Download de Resultado**: `GET /api/download/{fileId}`
- **Listar Arquivos**: `GET /api/files`
- **Status do Arquivo**: `GET /api/status/{fileId}`

### WebSocket

- **Endpoint**: `ws://localhost:8080/ws`
- **Tópico**: `/topic/processing-notifications`

Exemplo de conexão WebSocket (JavaScript):

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    stompClient.subscribe('/topic/processing-notifications', function(message) {
        const notification = JSON.parse(message.body);
        console.log('Notificação:', notification);
    });
});
```

## 📨 Formato de Mensagem Kafka

O consumidor espera mensagens JSON no tópico `project-structure`:

```json
{
  "project_id": "projeto-123",
  "project_name": "Meu Projeto",
  "timestamp": "2026-01-27T10:00:00",
  "root_path": "/path/to/project",
  "files": [
    {
      "name": "app.py",
      "path": "src/app.py",
      "size": 1024,
      "extension": ".py"
    }
  ],
  "directories": [
    {
      "name": "src",
      "path": "src",
      "file_count": 10
    }
  ],
  "statistics": {
    "total_files": 50,
    "total_directories": 10,
    "total_size": 102400
  }
}
```

## 🧪 Testando

### Enviar Mensagem de Teste ao Kafka

```bash
# Acessar container Kafka
docker exec -it kafka bash

# Criar tópico (se não existir)
kafka-topics --create --topic project-structure \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

# Enviar mensagem de teste
kafka-console-producer --topic project-structure \
  --bootstrap-server localhost:9092
```

Cole o JSON de exemplo e pressione Enter.

## 📊 Monitoramento

### Kafka UI

Acesse: http://localhost:8090

Visualize tópicos, mensagens, consumer groups, etc.

### Logs

```bash
# Ver logs da aplicação
docker-compose logs -f kafka-llm-processor

# Ver logs do Kafka
docker-compose logs -f kafka
```

## 🔧 Desenvolvimento

### Compilar

```bash
mvn clean compile
```

### Executar Testes

```bash
mvn test
```

### Build Docker

```bash
docker build -t kafka-llm-processor:latest .
```

## 📝 Formato de Saída

Os resultados são salvos em `./output/{projectId}_{timestamp}.json`:

```json
{
  "projectId": "projeto-123",
  "projectName": "Meu Projeto",
  "fileId": "abc123",
  "analysis": "Análise completa do LLM...",
  "processedAt": "2026-01-27T10:05:00",
  "status": "SUCCESS",
  "filePath": "/app/output/projeto-123_20260127100500.json",
  "processingTimeMs": 5000
}
```

## 🐛 Troubleshooting

### Kafka não conecta

```bash
# Verificar se Kafka está rodando
docker-compose ps

# Reiniciar Kafka
docker-compose restart kafka
```

### Erro de API Key

Certifique-se de que a variável `ANTHROPIC_API_KEY` está configurada corretamente.

## 📄 Licença

MIT License

## 👥 Contribuindo

1. Fork o projeto
2. Crie sua feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request
