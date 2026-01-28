#!/bin/bash

# Script para parar todos os serviços do Sistema de Análise de Projetos
# Backend (Java/Spring Boot) + Frontend (Python/Streamlit) + Kafka

echo "🛑 Parando Sistema de Análise de Projetos..."
echo "============================================="
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

# Função para parar processos Streamlit
stop_streamlit() {
    echo "🎨 Parando aplicação Streamlit..."
    
    # Encontrar processos Streamlit
    STREAMLIT_PIDS=$(pgrep -f "streamlit run app.py")
    
    if [ -n "$STREAMLIT_PIDS" ]; then
        echo "   Encontrados processos: $STREAMLIT_PIDS"
        for pid in $STREAMLIT_PIDS; do
            echo "   Parando PID: $pid"
            kill $pid 2>/dev/null
            sleep 1
            # Forçar se necessário
            if ps -p $pid > /dev/null 2>&1; then
                kill -9 $pid 2>/dev/null
            fi
        done
        echo -e "${GREEN}✅ Streamlit parado${NC}"
    else
        echo -e "${YELLOW}⚠️  Nenhum processo Streamlit encontrado${NC}"
    fi
    echo ""
}

# Parar Frontend (Streamlit)
if pgrep -f "streamlit run" > /dev/null 2>&1; then
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${BLUE}🎨 FRONTEND (Streamlit)${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    stop_streamlit
fi

# Parar Backend (Docker Compose)
if [ -f "$BACKEND_DIR/docker-compose.yml" ]; then
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${BLUE}📦 BACKEND (Docker Compose)${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    cd "$BACKEND_DIR"
    
    echo "🐳 Parando containers do backend..."
    docker-compose down
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Backend parado${NC}"
    else
        echo -e "${RED}❌ Erro ao parar backend${NC}"
    fi
    echo ""
    
    cd "$PROJECT_ROOT"
fi

# Parar Infraestrutura (Docker Compose raiz)
if [ -f "$PROJECT_ROOT/docker-compose.yml" ]; then
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${BLUE}🔧 INFRAESTRUTURA (Kafka + Zookeeper)${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    cd "$PROJECT_ROOT"
    
    echo "🐳 Parando containers da infraestrutura..."
    docker-compose down
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Infraestrutura parada${NC}"
    else
        echo -e "${RED}❌ Erro ao parar infraestrutura${NC}"
    fi
    echo ""
fi

# Verificar containers Docker restantes
echo "🔍 Verificando containers Docker..."
RUNNING_CONTAINERS=$(docker ps -q --filter "name=kafka" --filter "name=zookeeper" --filter "name=kafka-llm-processor" --filter "name=kafka-ui")

if [ -n "$RUNNING_CONTAINERS" ]; then
    echo -e "${YELLOW}⚠️  Encontrados containers ainda em execução${NC}"
    read -p "   Deseja forçar a parada desses containers? (s/N): " force_stop
    
    if [ "$force_stop" = "s" ] || [ "$force_stop" = "S" ]; then
        echo "   Forçando parada dos containers..."
        docker stop $RUNNING_CONTAINERS
        docker rm $RUNNING_CONTAINERS 2>/dev/null
        echo -e "${GREEN}✅ Containers forçados a parar${NC}"
    fi
else
    echo -e "${GREEN}✅ Nenhum container relacionado em execução${NC}"
fi

echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✨ TODOS OS SERVIÇOS FORAM PARADOS!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Para iniciar novamente, execute: ./start.sh"
echo ""

# Opções adicionais
read -p "Deseja também remover volumes Docker? (s/N): " remove_volumes
if [ "$remove_volumes" = "s" ] || [ "$remove_volumes" = "S" ]; then
    echo ""
    echo "🗑️  Removendo volumes Docker..."
    
    cd "$BACKEND_DIR"
    docker-compose down -v 2>/dev/null
    
    cd "$PROJECT_ROOT"
    docker-compose down -v 2>/dev/null
    
    echo -e "${GREEN}✅ Volumes removidos${NC}"
fi

echo ""
read -p "Deseja limpar arquivos de log e temporários? (s/N): " cleanup
if [ "$cleanup" = "s" ] || [ "$cleanup" = "S" ]; then
    echo ""
    echo "🗑️  Limpando arquivos temporários..."
    
    # Remover logs
    rm -f "$PROJECT_ROOT"/*.log 2>/dev/null
    rm -f "$BACKEND_DIR"/*.log 2>/dev/null
    rm -f "$FRONTEND_DIR"/*.log 2>/dev/null
    
    # Remover PIDs
    rm -f "$PROJECT_ROOT"/*.pid 2>/dev/null
    rm -f "$BACKEND_DIR"/*.pid 2>/dev/null
    rm -f "$FRONTEND_DIR"/*.pid 2>/dev/null
    
    echo -e "${GREEN}✅ Arquivos temporários removidos${NC}"
fi

echo ""
