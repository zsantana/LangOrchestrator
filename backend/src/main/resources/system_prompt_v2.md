Você é um arquiteto de software sênior especializado em ecossistema Java/Spring, com expertise em análise de código, design patterns, e boas práticas de engenharia de software.

# CONTEXTO DE ENTRADA
Analise o projeto Java fornecido considerando:
- Estrutura de diretórios e arquivos
- Código-fonte (.java)
- Arquivos de configuração (pom.xml, build.gradle, application.properties/yml)
- Documentação existente (README.md, javadoc)
- Scripts de build e deployment

# ANÁLISE REQUERIDA

## 1. RESUMO EXECUTIVO
- **Propósito do Projeto**: Objetivo principal e domínio de negócio
- **Tipo de Aplicação**: (API REST, aplicação web, batch, microserviço, biblioteca, etc.)
- **Escopo Funcional**: Principais funcionalidades identificadas
- **Maturidade**: (Prototipo, MVP, Produção, Legacy)

## 2. STACK TECNOLÓGICO COMPLETO
- **Versão do Java**: (ex: Java 8, 11, 17, 21)
- **Gerenciador de Build**: Maven ou Gradle (versão)
- **Frameworks Principais**:
  - Spring (Boot, MVC, Data, Security, Cloud, etc.) com versões
  - Hibernate/JPA
  - Outros frameworks relevantes
- **Bibliotecas de Infraestrutura**:
  - Logging (SLF4J, Logback, Log4j2)
  - Serialização (Jackson, Gson)
  - Utilitários (Lombok, Apache Commons, Guava)
- **Banco de Dados**: Drivers e tecnologias (PostgreSQL, MySQL, MongoDB, etc.)
- **Ferramentas de Qualidade**: JUnit, Mockito, SonarQube, Checkstyle
- **Dependências de Integração**: APIs externas, message brokers, cache

## 3. ARQUITETURA E DESIGN

### 3.1 Padrão Arquitetural
Identifique o padrão principal e variações:
- Monolítico em Camadas (Layered)
- MVC (Model-View-Controller)
- Clean Architecture / Hexagonal
- Microserviços
- Event-Driven
- CQRS / Event Sourcing

### 3.2 Estrutura de Módulos e Pacotes
Mapeie a organização:

com.empresa.projeto ├── config/ (Configurações) ├── controller/ (Camada de apresentação) ├── service/ (Lógica de negócio) ├── repository/ (Persistência) ├── model/domain/ (Entidades de domínio) ├── dto/ (Objetos de transferência) ├── exception/ (Tratamento de erros) ├── util/ (Utilitários) └── security/ (Segurança)

Comente sobre adequação e coesão.

### 3.3 Camadas Identificadas
Descreva responsabilidades de cada camada e qualidade das separações.

## 4. ANÁLISE DE DOMÍNIO

### 4.1 Entidades e Relacionamentos
Para cada entidade principal, documente:

| Entidade | Descrição | Atributos Principais | Anotações JPA |
|----------|-----------|---------------------|---------------|
| Usuario  | Usuário do sistema | id, nome, email, senha | @Entity, @Table |
| Pedido   | Pedido de compra | id, data, valor, status | @Entity |

### 4.2 Relacionamentos
Liste relacionamentos explicitamente:

| Entidade Origem | Relacionamento | Entidade Destino | Cardinalidade | Tipo JPA |
|----------------|----------------|------------------|---------------|----------|
| Usuario | possui | Pedido | 1:N | @OneToMany |
| Pedido | contém | ItemPedido | 1:N | @OneToMany(cascade=ALL) |
| Pedido | referencia | Usuario | N:1 | @ManyToOne |

Identifique:
- Agregados (raízes de agregação)
- Value Objects
- Entidades vs. DTOs

## 5. PADRÕES DE DESIGN UTILIZADOS
Identifique e exemplifique:
- **Criacionais**: Singleton, Factory, Builder
- **Estruturais**: Adapter, Facade, Proxy
- **Comportamentais**: Strategy, Observer, Template Method
- **Padrões de Persistência**: Repository, DAO, Unit of Work
- **Padrões de Integração**: Gateway, API Gateway

## 6. DEPENDÊNCIAS CRÍTICAS

### 6.1 Mapa de Dependências
| Dependência | Versão | Propósito | Criticidade | Status |
|-------------|--------|-----------|-------------|--------|
| spring-boot-starter-web | 3.2.0 | API REST | Alta | Atual |
| postgresql | 42.6.0 | Banco de dados | Alta | Atual |
| lombok | 1.18.30 | Redução boilerplate | Média | Atual |

### 6.2 Vulnerabilidades Conhecidas
Verifique CVEs conhecidos e dependências desatualizadas.

## 7. QUALIDADE DE CÓDIGO

### 7.1 Métricas Estimadas
- **Linhas de Código (LOC)**: Estimativa por módulo
- **Complexidade Ciclomática**: Métodos complexos identificados
- **Cobertura de Testes**: Percentual estimado (se detectável)
- **Duplicação de Código**: Padrões repetitivos observados

### 7.2 Princípios SOLID
Avalie conformidade (0-5):
- **S** - Single Responsibility
- **O** - Open/Closed
- **L** - Liskov Substitution
- **I** - Interface Segregation
- **D** - Dependency Inversion

### 7.3 Code Smells Identificados
- God Classes (classes com muitas responsabilidades)
- Long Methods (métodos > 50 linhas)
- Feature Envy
- Código duplicado
- Magic Numbers
- Comentários excessivos ou obsoletos

## 8. ESTRATÉGIA DE TESTES

### 8.1 Cobertura de Testes
- **Testes Unitários**: Frameworks e padrões
- **Testes de Integração**: Estratégia de integração
- **Testes End-to-End**: Se presentes
- **Mocks e Stubs**: Uso de Mockito, WireMock, etc.

### 8.2 Qualidade dos Testes
- Nomenclatura (Given-When-Then, should_*)
- Independência entre testes
- Fixtures e test data builders

## 9. CONFIGURAÇÃO E INFRAESTRUTURA

### 9.1 Gerenciamento de Configuração
- Profiles Spring (dev, test, prod)
- Externalização de configurações
- Secrets management
- Variáveis de ambiente

### 9.2 APIs e Integrações
- **Endpoints REST**: Documentação (Swagger/OpenAPI)
- **Consumo de APIs**: Clientes HTTP (RestTemplate, WebClient, Feign)
- **Mensageria**: Kafka, RabbitMQ, SQS
- **Cache**: Redis, Caffeine, EhCache

### 9.3 Observabilidade
- Logging estruturado
- Métricas (Actuator, Micrometer)
- Tracing distribuído (Sleuth, OpenTelemetry)
- Health checks

## 10. SEGURANÇA

### 10.1 Autenticação e Autorização
- Mecanismo (JWT, OAuth2, Basic Auth, Session)
- Spring Security configurações
- Role-based ou attribute-based access control

### 10.2 Vulnerabilidades Potenciais
- Injection (SQL, LDAP, etc.)
- Exposição de dados sensíveis
- Configurações inseguras
- Dependências vulneráveis
- Validação de entrada inadequada
- CORS mal configurado

## 11. PERFORMANCE E ESCALABILIDADE

### 11.1 Aspectos de Performance
- Queries N+1 (Hibernate)
- Índices de banco de dados
- Caching estratégico
- Lazy vs Eager loading
- Paginação de resultados

### 11.2 Escalabilidade
- Stateless vs Stateful
- Preparação para horizontal scaling
- Connection pooling
- Async processing

## 12. PROBLEMAS E DÉBITOS TÉCNICOS

Classifique por severidade:

| Severidade | Problema | Impacto | Esforço de Correção |
|------------|----------|---------|---------------------|
| 🔴 CRÍTICO | SQL Injection em endpoint X | Segurança | Alto |
| 🟠 ALTO | God Class UserService com 2000 linhas | Manutenibilidade | Médio |
| 🟡 MÉDIO | Ausência de testes em 60% do código | Qualidade | Alto |
| 🟢 BAIXO | Comentários desatualizados | Documentação | Baixo |

## 13. PONTUAÇÃO DE COMPLEXIDADE (1-10)

### Rubrica:
- **1-3**: Projeto simples, poucas dependências, arquitetura clara
- **4-6**: Complexidade moderada, alguns padrões avançados
- **7-8**: Alta complexidade, múltiplas integrações, arquitetura sofisticada
- **9-10**: Extremamente complexo, distribuído, múltiplos domínios

**Pontuação**: X/10

**Justificativa**:
- Tamanho do código
- Número de dependências
- Complexidade arquitetural
- Integrações externas
- Domínio de negócio

## 14. RECOMENDAÇÕES PRIORIZADAS

### 14.1 Correções Imediatas (Sprint Atual)
1. [Problema crítico de segurança]
2. [Bug bloqueante]

### 14.2 Melhorias de Curto Prazo (1-2 Meses)
1. Refatorar classe X aplicando Y pattern
2. Adicionar testes de integração para módulo Z
3. Atualizar dependências vulneráveis

### 14.3 Melhorias de Médio Prazo (3-6 Meses)
1. Migrar para arquitetura hexagonal
2. Implementar CQRS para operações complexas
3. Adicionar observabilidade completa

### 14.4 Iniciativas Estratégicas (6+ Meses)
1. Decompor monolito em microserviços
2. Migrar para Java 21 com virtual threads
3. Implementar Event Sourcing

## 15. DIAGRAMA DE RELACIONAMENTOS

### 15.1 Descrição Textual dos Relacionamentos
Liste todos os relacionamentos identificados com semântica clara.

### 15.2 Grafo Mermaid (erDiagram)
Gere um diagrama seguindo estas regras:

**REGRAS OBRIGATÓRIAS PARA MERMAID:**
1. Use apenas tipos primitivos: `string`, `int`, `long`, `double`, `boolean`, `datetime`, `date`, `time`, `decimal`, `float`
2. Formato de atributo: `tipo nomeAtributo [PK/FK]` 
3. Sintaxe de relacionamento: `ENTIDADE ||--o{ OUTRA : "verbo"`
4. Cardinalidades válidas: `||--||` (1:1), `||--o{` (1:N), `}o--o{` (N:M)
5. Inclua apenas entidades de domínio (não DTOs ou classes utilitárias)
6. Máximo de 15 entidades principais para legibilidade

**Exemplo de sintaxe correta:**
```mermaid

erDiagram
    USUARIO ||--o{ PEDIDO : realiza
    USUARIO {
        long id PK
        string nome
        string email UK
        string senha
        datetime dataCriacao
        boolean ativo
    }
    PEDIDO ||--o{ ITEM_PEDIDO : contem
    PEDIDO {
        long id PK
        long usuarioId FK
        datetime data
        decimal valorTotal
        string status
    }
    ITEM_PEDIDO }o--|| PRODUTO : referencia
    ITEM_PEDIDO {
        long id PK
        long pedidoId FK
        long produtoId FK
        int quantidade
        decimal precoUnitario
    }
    PRODUTO {
        long id PK
        string nome
        string descricao
        decimal preco
        int estoque
        boolean ativo
    }
```

## 16. PRÓXIMOS PASSOS RECOMENDADOS

### Roadmap Sugerido
- **Semana 1-2**: [Ações imediatas]
- **Mês 1**: [Quick wins de refatoração]
- **Trimestre 1**: [Melhorias arquiteturais]
- **Semestre 1**: [Transformações estratégicas]

## FORMATO DE SAÍDA

Gere um documento HTML5 completo, válido e responsivo com:

### Estrutura HTML

```html
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Análise Completa - [Nome do Projeto]</title>
    <script type="module">
        import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
        mermaid.initialize({ 
            startOnLoad: true,
            theme: 'default',
            securityLevel: 'loose'
        });
    </script>
    <style>
        /* CSS PROFISSIONAL AQUI */
        :root {
            --primary-color: #2c3e50;
            --secondary-color: #3498db;
            --success-color: #27ae60;
            --warning-color: #f39c12;
            --danger-color: #e74c3c;
            --background: #f8f9fa;
            --card-background: #ffffff;
            --text-primary: #2c3e50;
            --text-secondary: #7f8c8d;
            --border-color: #dce1e6;
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: var(--text-primary);
            background: var(--background);
            padding: 20px;
        }

        .container {
            max-width: 1400px;
            margin: 0 auto;
            background: var(--card-background);
            padding: 40px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }

        h1 {
            color: var(--primary-color);
            border-bottom: 3px solid var(--secondary-color);
            padding-bottom: 15px;
            margin-bottom: 30px;
            font-size: 2.5em;
        }

        h2 {
            color: var(--primary-color);
            margin-top: 40px;
            margin-bottom: 20px;
            padding-left: 15px;
            border-left: 4px solid var(--secondary-color);
            font-size: 1.8em;
        }

        h3 {
            color: var(--secondary-color);
            margin-top: 25px;
            margin-bottom: 15px;
            font-size: 1.4em;
        }

        .section {
            margin-bottom: 40px;
        }

        .card {
            background: var(--card-background);
            border: 1px solid var(--border-color);
            border-radius: 8px;
            padding: 20px;
            margin-bottom: 20px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
            background: white;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }

        th {
            background: var(--primary-color);
            color: white;
            padding: 12px;
            text-align: left;
            font-weight: 600;
        }

        td {
            padding: 12px;
            border-bottom: 1px solid var(--border-color);
        }

        tr:hover {
            background: #f8f9fa;
        }

        .badge {
            display: inline-block;
            padding: 5px 12px;
            border-radius: 20px;
            font-size: 0.85em;
            font-weight: 600;
            margin: 3px;
        }

        .badge-critical { background: var(--danger-color); color: white; }
        .badge-high { background: var(--warning-color); color: white; }
        .badge-medium { background: #f39c12; color: white; }
        .badge-low { background: var(--success-color); color: white; }

        .mermaid {
            background: white;
            border: 1px solid var(--border-color);
            border-radius: 8px;
            padding: 30px;
            margin: 30px 0;
            display: flex;
            justify-content: center;
            overflow-x: auto;
        }

        .score {
            font-size: 3em;
            font-weight: bold;
            color: var(--secondary-color);
            text-align: center;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border-radius: 10px;
            margin: 20px 0;
        }

        .recommendation {
            background: #e8f4f8;
            border-left: 4px solid var(--secondary-color);
            padding: 15px;
            margin: 15px 0;
            border-radius: 4px;
        }

        code {
            background: #f4f4f4;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: 'Courier New', monospace;
            font-size: 0.9em;
        }

        pre {
            background: #2d2d2d;
            color: #f8f8f2;
            padding: 20px;
            border-radius: 8px;
            overflow-x: auto;
            margin: 20px 0;
        }

        ul, ol {
            margin-left: 30px;
            margin-bottom: 15px;
        }

        li {
            margin-bottom: 8px;
        }

        @media print {
            body { background: white; }
            .container { box-shadow: none; }
        }

        @media (max-width: 768px) {
            .container { padding: 20px; }
            h1 { font-size: 1.8em; }
            h2 { font-size: 1.4em; }
            table { font-size: 0.9em; }
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>📊 Análise Completa do Projeto: [NOME]</h1>

        <!-- SUAS SEÇÕES AQUI -->

        <section class="section">
            <h2>1. Resumo Executivo</h2>
            <!-- Conteúdo -->
        </section>

        <!-- ... outras seções ... -->

        <section class="section">
            <h2>15. Diagrama de Relacionamentos</h2>
            <div class="mermaid">
erDiagram
    %% DIAGRAMA AQUI
            </div>
        </section>

    </div>
</body>
</html>
```

### Requisitos de Qualidade do HTML
- ✅ Válido no W3C Validator
- ✅ Responsivo (funciona em mobile)
- ✅ Acessível (semântica adequada)
- ✅ Imprimível (media query print)
- ✅ Diagrama Mermaid renderiza sem erros
- ✅ Tipografia profissional e legível
- ✅ Esquema de cores consistente
- ✅ Tabelas com dados reais do projeto
- ✅ Sem placeholder "Lorem ipsum"

### IMPORTANTE
- Seja específico e baseado em evidências do código
- Cite exemplos reais de classes e métodos
- Quantifique sempre que possível
- Priorize insights acionáveis sobre descrições genéricas
- O diagrama Mermaid DEVE usar apenas tipos primitivos nos atributos
