# Agente Multi-Modal - Sistema de Análise de Projetos

Sistema completo de análise de estrutura de projetos utilizando processamento assíncrono com Kafka e análise inteligente com LLM (Large Language Model).

## 📖 Visão Geral

Este sistema permite fazer upload de projetos (em formato ZIP), extrair e mapear sua estrutura, processar através de um LLM para análise detalhada, e visualizar os resultados em tempo real através de uma interface web interativa.

### Arquitetura do Sistema

```
┌─────────────────┐         ┌──────────────┐         ┌─────────────────┐
│                 │         │              │         │                 │
│  Frontend       │────────▶│    Kafka     │────────▶│    Backend      │
│  (Streamlit)    │         │   Message    │         │  (Spring Boot)  │
│                 │         │    Broker    │         │                 │
└─────────────────┘         └──────────────┘         └─────────────────┘
        │                                                      │
        │                   WebSocket                         │
        │◀────────────────────────────────────────────────────┘
        │                 (Notificações)
```

**Fluxo de Processamento:**

1. **Frontend**: Usuário faz upload de arquivo ZIP do projeto
2. **Extração**: Sistema extrai e mapeia estrutura de arquivos
3. **Kafka**: Estrutura é enviada para tópico Kafka
4. **Backend**: Consome mensagem e processa com LLM (Claude AI)
5. **Armazenamento**: Resultado salvo em JSON
6. **Notificação**: WebSocket notifica frontend em tempo real
7. **Download**: Usuário pode baixar resultado processado

---

## 📁 Estrutura do Repositório

```
agente-multi-modal/
│
├── backend/                    # Processador Java/Spring Boot
│   ├── src/                   # Código-fonte Java
│   ├── pom.xml               # Dependências Maven
│   ├── Dockerfile            # Container backend
│   ├── docker-compose.yml    # Stack completa (Kafka + Backend)
│   └── README.md             # Documentação detalhada
│
├── frontend/                   # Interface Streamlit Python
│   ├── app.py                # Aplicação principal Streamlit
│   ├── config.py             # Configurações
│   ├── project_analyzer.py   # Análise de estrutura de projetos
│   ├── kafka_client.py       # Cliente Kafka Producer
│   ├── websocket_client.py   # Cliente WebSocket
│   ├── download_client.py    # Cliente de download
│   ├── requirements.txt      # Dependências Python
│   ├── uploads/              # Arquivos enviados
│   ├── extracted/            # Projetos extraídos
│   └── processed/            # Resultados processados
│
├── docker-compose.yml          # Orquestração completa do sistema
├── start.sh                    # Script para iniciar todo sistema
├── stop.sh                     # Script para parar todo sistema
├── .gitignore                  # Arquivos ignorados pelo Git
└── README.md                   # Este arquivo
```

---

## 🎯 Componentes do Sistema

### 1️⃣ Frontend (Python + Streamlit)

**Localização:** `frontend/`

Interface web interativa construída com Streamlit para gerenciar upload, processamento e visualização de análises de projetos.

#### Funcionalidades Principais:

- ✅ **Upload de Projetos**: Interface drag-and-drop para arquivos ZIP
- 🗂️ **Extração e Mapeamento**: Análise automática da estrutura de arquivos
- 📤 **Envio para Kafka**: Publicação de mensagens no broker
- 🔔 **Notificações em Tempo Real**: WebSocket para atualizações ao vivo
- 📊 **Visualização**: Dashboard com histórico e estatísticas
- 💾 **Download**: Exportação de resultados em JSON

#### Tecnologias:

- **Streamlit**: Framework de interface web
- **Kafka-Python**: Cliente Kafka para Python
- **WebSocket-Client**: Conexão WebSocket
- **Requests**: Cliente HTTP para downloads

#### Como Executar:

```bash
cd frontend
pip install -r requirements.txt
streamlit run app.py
```

**URL:** http://localhost:8501

---

### 2️⃣ Backend (Java + Spring Boot)

**Localização:** `backend/`

Processador robusto que consome mensagens do Kafka, analisa estruturas de projetos usando LLM e notifica resultados via WebSocket.

#### Funcionalidades Principais:

- 📨 **Consumer Kafka**: Consome mensagens do tópico `project-structure`
- 🤖 **Integração LLM**: Processamento com Anthropic Claude AI
- 💾 **Armazenamento**: Salva análises em arquivos JSON
- 🔔 **WebSocket**: Notificações em tempo real
- 🌐 **API REST**: Endpoint para download de arquivos processados

#### Tecnologias:

- **Spring Boot 3.3**: Framework principal
- **Spring Kafka**: Integração com Kafka
- **Spring AI**: Integração com Anthropic Claude
- **Spring WebSocket**: Comunicação bidirecional
- **Java 21**: Linguagem de programação

#### Estrutura de Código:

```
backend/src/main/java/com/processor/kafkallm/
│
├── KafkaLlmProcessorApplication.java    # Aplicação principal
│
├── config/                               # Configurações
│   ├── AppConfig.java
│   ├── KafkaConsumerConfig.java
│   └── WebSocketConfig.java
│
├── model/                                # Modelos de dados
│   ├── ProjectStructure.java
│   ├── ProcessingResult.java
│   └── ProcessingNotification.java
│
├── service/                              # Lógica de negócio
│   ├── LlmProcessingService.java
│   ├── StorageService.java
│   └── WebSocketNotificationService.java
│
├── kafka/                                # Consumer Kafka
│   └── ProjectStructureConsumer.java
│
└── controller/                           # REST API
    └── DownloadController.java
```

#### Como Executar:

**Com Docker (Recomendado):**
```bash
cd backend
docker-compose up -d
```

**Localmente:**
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

**Porta:** 8080

---

## 🚀 Executando o Sistema Completo

### Pré-requisitos

- **Docker** e **Docker Compose**
- **Java 21** (se executar backend localmente)
- **Python 3.8+** (se executar frontend localmente)
- **Chave API Anthropic Claude**

### Opção 1: Usando Scripts (Recomendado)

#### Iniciar Sistema:
```bash
./start.sh
```

Isso iniciará:
- Zookeeper (porta 2181)
- Kafka (porta 9092)
- Kafka UI (porta 9000)
- Backend (porta 8080)

#### Parar Sistema:
```bash
./stop.sh
```

### Opção 2: Docker Compose Manual

```bash
# Iniciar toda a stack
docker-compose up -d

# Ver logs
docker-compose logs -f

# Parar tudo
docker-compose down
```

### Opção 3: Execução Local (Desenvolvimento)

**1. Iniciar Kafka:**
```bash
docker-compose up -d zookeeper kafka kafka-ui
```

**2. Iniciar Backend:**
```bash
cd backend
export ANTHROPIC_API_KEY=sua_chave_api
mvn spring-boot:run
```

**3. Iniciar Frontend:**
```bash
cd frontend
source ../venv/bin/activate  # ou ative seu ambiente virtual
streamlit run app.py
```

---

## 🔧 Configuração

### Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto:

```env
# API Anthropic
ANTHROPIC_API_KEY=sua_chave_api_aqui

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC=project-structure

# WebSocket
WEBSOCKET_URL=ws://localhost:8080/ws-native

# API
API_DOWNLOAD_URL=http://localhost:8080/download
```

### Configuração do Backend

Edite `backend/src/main/resources/application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: llm-processor-group

anthropic:
  api-key: ${ANTHROPIC_API_KEY}
```

---

## 📊 Uso do Sistema

### 1. Acessar Interface

Abra o navegador em: **http://localhost:8501**

### 2. Fazer Upload

- Clique em "Upload de Projeto"
- Selecione arquivo ZIP do seu projeto
- Aguarde extração automática

### 3. Processar

- Revise a estrutura mapeada
- Clique em "Enviar para Processamento"
- Acompanhe notificações em tempo real

### 4. Visualizar Resultados

- Acesse aba "Resultados"
- Veja análise detalhada do LLM
- Faça download do JSON

### 5. Monitorar Kafka (Opcional)

Acesse Kafka UI: **http://localhost:9000**

---

## 📦 Dependências Principais

### Frontend

- `streamlit` - Interface web
- `kafka-python` - Cliente Kafka
- `websocket-client` - WebSocket
- `requests` - Cliente HTTP

### Backend

- `spring-boot-starter-web` - REST API
- `spring-kafka` - Integração Kafka
- `spring-ai-anthropic` - Claude AI
- `spring-boot-starter-websocket` - WebSocket

---

## 🧪 Testes

### Backend

```bash
cd backend
mvn test
```

### Frontend

```bash
cd frontend
pytest  # se testes estiverem configurados
```

---

## 📝 Formato de Dados

### Mensagem Kafka (Entrada)

```json
{
  "projectId": "project_20240128_123456",
  "projectName": "meu-projeto",
  "structure": {
    "files": [
      {
        "path": "src/main/java/App.java",
        "content": "public class App { ... }"
      }
    ],
    "directories": ["src", "target"]
  },
  "timestamp": "2024-01-28T12:34:56"
}
```

### Resultado Processado (Saída)

```json
{
  "projectId": "project_20240128_123456",
  "analysis": "Análise detalhada do LLM...",
  "recommendations": ["...", "..."],
  "timestamp": "2024-01-28T12:35:30",
  "status": "completed"
}
```

---

## 🐛 Troubleshooting

### Kafka não está conectando

```bash
# Verificar se Kafka está rodando
docker ps | grep kafka

# Recriar containers
docker-compose down -v
docker-compose up -d
```

### Backend não processa mensagens

```bash
# Verificar logs
docker-compose logs backend

# Verificar chave API
echo $ANTHROPIC_API_KEY
```

### WebSocket não conecta

- Verificar se backend está rodando na porta 8080
- Verificar firewall/antivírus
- Confirmar URL no `config.py` do frontend

---

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/MinhaFeature`)
3. Commit suas mudanças (`git commit -m 'Adiciona MinhaFeature'`)
4. Push para a branch (`git push origin feature/MinhaFeature`)
5. Abra um Pull Request

---

## 📄 Licença

Este projeto é de uso educacional e demonstrativo.

---

## 👥 Autores

Desenvolvido como sistema de demonstração de integração Kafka + LLM.

---

## 📞 Suporte

Para problemas ou dúvidas:
- Consulte a documentação detalhada em `backend/README.md`
- Verifique issues no repositório
- Revise logs dos containers Docker

---

## 🔗 Links Úteis

- [Documentação Spring Kafka](https://spring.io/projects/spring-kafka)
- [Streamlit Docs](https://docs.streamlit.io)
- [Anthropic Claude API](https://docs.anthropic.com)
- [Apache Kafka](https://kafka.apache.org/documentation/)
