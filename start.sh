#!/bin/bash

# Script de inicialização do Sistema de Processamento de Projetos
# Backend (Java/Spring Boot) + Frontend (Python/Streamlit) + Kafka

echo "🚀 Sistema de Análise de Projetos - Inicialização"
echo "=================================================="
echo ""

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Diretório raiz do projeto
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
FRONTEND_DIR="$PROJECT_ROOT/frontend"

# Função para verificar se um comando existe
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Verificar dependências
echo "🔍 Verificando dependências..."

if ! command_exists docker; then
    echo -e "${RED}❌ Docker não está instalado${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker encontrado${NC}"

if ! command_exists docker-compose; then
    echo -e "${RED}❌ Docker Compose não está instalado${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker Compose encontrado${NC}"

if ! command_exists python3; then
    echo -e "${RED}❌ Python 3 não está instalado${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Python 3 encontrado${NC}"

echo ""

# Verificar arquivo .env
if [ ! -f "$BACKEND_DIR/.env" ]; then
    echo -e "${YELLOW}⚠️  Arquivo .env não encontrado no backend${NC}"
    echo "   Certifique-se de configurar ANTHROPIC_API_KEY"
    read -p "   Deseja continuar? (s/N): " continue_without_env
    if [ "$continue_without_env" != "s" ] && [ "$continue_without_env" != "S" ]; then
        exit 1
    fi
else
    echo -e "${GREEN}✅ Arquivo .env configurado${NC}"
fi

echo ""

# Menu de opções
echo "📋 Selecione o modo de inicialização:"
echo ""
echo "1) Sistema Completo (Backend + Frontend + Kafka)"
echo "2) Apenas Backend (Kafka + Processador Java)"
echo "3) Apenas Frontend (Streamlit)"
echo "4) Infraestrutura (Apenas Kafka + Zookeeper + Kafka UI)"
echo ""
read -p "Escolha uma opção (1-4): " option

echo ""

case $option in
    1)
        echo -e "${BLUE}🚀 Iniciando sistema completo...${NC}"
        START_BACKEND=true
        START_FRONTEND=true
        ;;
    2)
        echo -e "${BLUE}🚀 Iniciando apenas backend...${NC}"
        START_BACKEND=true
        START_FRONTEND=false
        ;;
    3)
        echo -e "${BLUE}🚀 Iniciando apenas frontend...${NC}"
        START_BACKEND=false
        START_FRONTEND=true
        ;;
    4)
        echo -e "${BLUE}🚀 Iniciando apenas infraestrutura...${NC}"
        START_BACKEND=false
        START_FRONTEND=false
        START_INFRA_ONLY=true
        ;;
    *)
        echo -e "${RED}❌ Opção inválida${NC}"
        exit 1
        ;;
esac

echo ""

# Iniciar Backend (Kafka + Processador Java)
if [ "$START_BACKEND" = true ]; then
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${BLUE}📦 BACKEND (Java/Spring Boot + Kafka)${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    cd "$BACKEND_DIR"
    
    echo "🐳 Iniciando containers Docker..."
    docker-compose up -d
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Backend iniciado com sucesso!${NC}"
        echo ""
        echo "   📊 Kafka UI: http://localhost:8090"
        echo "   🔌 Backend API: http://localhost:8080"
        echo "   🌐 WebSocket: ws://localhost:8080/ws-native"
        echo "   🩺 Health Check: http://localhost:8080/actuator/health"
        echo ""
        
        # Aguardar backend ficar pronto
        echo "⏳ Aguardando backend inicializar..."
        sleep 10
        
        # Verificar saúde do backend
        echo "🔍 Verificando saúde do backend..."
        for i in {1..30}; do
            if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
                echo -e "${GREEN}✅ Backend está saudável!${NC}"
                break
            fi
            if [ $i -eq 30 ]; then
                echo -e "${YELLOW}⚠️  Backend pode ainda estar inicializando...${NC}"
            else
                echo -n "."
                sleep 2
            fi
        done
        echo ""
    else
        echo -e "${RED}❌ Erro ao iniciar backend${NC}"
        exit 1
    fi
    
    cd "$PROJECT_ROOT"
fi

# Iniciar apenas infraestrutura
if [ "$START_INFRA_ONLY" = true ]; then
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${BLUE}🔧 INFRAESTRUTURA (Kafka + Zookeeper)${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    cd "$PROJECT_ROOT"
    
    echo "🐳 Iniciando containers Docker..."
    docker-compose up -d
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Infraestrutura iniciada!${NC}"
        echo ""
        echo "   📊 Kafka UI: http://localhost:9000"
        echo "   📡 Kafka Broker: localhost:9092"
        echo "   🗄️  Zookeeper: localhost:2181"
        echo ""
    else
        echo -e "${RED}❌ Erro ao iniciar infraestrutura${NC}"
        exit 1
    fi
fi

# Iniciar Frontend (Streamlit)
if [ "$START_FRONTEND" = true ]; then
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${BLUE}🎨 FRONTEND (Python/Streamlit)${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    cd "$FRONTEND_DIR"
    
    # Criar diretórios necessários
    echo "📁 Criando diretórios necessários..."
    mkdir -p uploads extracted processed processed_files
    echo -e "${GREEN}✅ Diretórios criados${NC}"
    echo ""
    
    # Verificar ambiente virtual
    if [ -d "$PROJECT_ROOT/venv" ]; then
        echo "🐍 Ativando ambiente virtual..."
        source "$PROJECT_ROOT/venv/bin/activate"
        echo -e "${GREEN}✅ Ambiente virtual ativado${NC}"
    else
        echo -e "${YELLOW}⚠️  Ambiente virtual não encontrado${NC}"
        echo "   Execute: python3 -m venv venv"
        echo ""
    fi
    
    # Instalar dependências Python
    echo "📦 Instalando dependências Python..."
    pip install -q -r requirements.txt
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Dependências instaladas${NC}"
    else
        echo -e "${RED}❌ Erro ao instalar dependências${NC}"
        exit 1
    fi
    echo ""
    
    # Iniciar Streamlit
    echo "🎨 Iniciando aplicação Streamlit..."
    echo ""
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "✨ SISTEMA PRONTO!"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "📱 Frontend (Streamlit): http://localhost:8501"
    
    if [ "$START_BACKEND" = true ]; then
        echo "🔌 Backend API: http://localhost:8080"
        echo "📊 Kafka UI: http://localhost:8090"
        echo "🌐 WebSocket: ws://localhost:8080/ws-native"
    fi
    
    echo ""
    echo -e "${YELLOW}Para parar os serviços, execute: ./stop.sh${NC}"
    echo ""
    
    streamlit run app.py
    
    cd "$PROJECT_ROOT"
else
    # Se não iniciar frontend, mostrar resumo
    echo ""
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "✨ SERVIÇOS INICIADOS!"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    
    if [ "$START_BACKEND" = true ]; then
        echo "🔌 Backend API: http://localhost:8080"
        echo "📊 Kafka UI: http://localhost:8090"
        echo "🌐 WebSocket: ws://localhost:8080/ws-native"
    fi
    
    if [ "$START_INFRA_ONLY" = true ]; then
        echo "📊 Kafka UI: http://localhost:9000"
        echo "📡 Kafka Broker: localhost:9092"
    fi
    
    echo ""
    echo -e "${YELLOW}Para parar os serviços, execute: ./stop.sh${NC}"
    echo ""
fi
