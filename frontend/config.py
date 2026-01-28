"""
Configurações e constantes do sistema de processamento de projetos.
"""
import os
from pathlib import Path

# Configurações Kafka
KAFKA_BOOTSTRAP_SERVERS = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')
KAFKA_TOPIC = os.getenv('KAFKA_TOPIC', 'project-structure')

# Configurações WebSocket
WEBSOCKET_URL = os.getenv('WEBSOCKET_URL', 'ws://localhost:8080/ws-native')
WEBSOCKET_TOPIC_GENERAL = '/topic/notifications'
WEBSOCKET_TOPIC_PROJECT = '/topic/project/'

# Configurações API
API_DOWNLOAD_URL = os.getenv('API_DOWNLOAD_URL', 'http://localhost:8000/download')

# Diretórios
UPLOAD_DIR = Path("uploads")
EXTRACT_DIR = Path("extracted")
PROCESSED_DIR = Path("processed")

# Configurações de filtro para projetos Spring Boot/Maven
ALLOWED_EXTENSIONS = {
    '.java',           # Código Java
    '.xml',            # pom.xml e outros XMLs de configuração
    '.properties',     # application.properties
    '.yaml',           # application.yaml
    '.yml'             # application.yml
}

ALLOWED_FILENAMES = {
    'pom.xml',
    'application.properties',
    'application.yaml',
    'application.yml',
    'application-dev.properties',
    'application-prod.properties',
    'application-test.properties',
    'application-dev.yaml',
    'application-prod.yaml',
    'application-test.yaml',
    'application-dev.yml',
    'application-prod.yml',
    'application-test.yml'
}

IGNORED_DIRECTORIES = {
    'target',          # Diretório de build do Maven
    '.mvn',            # Configurações Maven
    'node_modules',
    '.git',
    '.idea',
    '.vscode',
    '__pycache__',
    'build',
    'dist'
}


def ensure_directories():
    """Cria os diretórios necessários se não existirem."""
    for dir_path in [UPLOAD_DIR, EXTRACT_DIR, PROCESSED_DIR]:
        dir_path.mkdir(exist_ok=True)
