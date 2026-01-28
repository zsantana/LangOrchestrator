"""
Módulo para análise e mapeamento de estrutura de projetos.
"""
import os
from pathlib import Path
from datetime import datetime
from typing import Dict, Optional

from config import (
    ALLOWED_EXTENSIONS,
    ALLOWED_FILENAMES,
    IGNORED_DIRECTORIES
)


def is_spring_boot_maven_project(root_path: Path) -> bool:
    """
    Verifica se é um projeto Spring Boot com Maven.
    
    Args:
        root_path: Caminho raiz do projeto
        
    Returns:
        True se for um projeto Spring Boot/Maven, False caso contrário
    """
    # Procura por pom.xml em qualquer subdiretório
    for pom_file in root_path.rglob('pom.xml'):
        try:
            with open(pom_file, 'r', encoding='utf-8') as f:
                content = f.read()
                # Verifica se contém dependências do Spring Boot
                if 'spring-boot' in content.lower():
                    return True
        except Exception:
            continue
    return False


def should_include_file(file_path: Path) -> bool:
    """
    Determina se um arquivo deve ser incluído no mapeamento.
    
    Args:
        file_path: Caminho do arquivo
        
    Returns:
        True se o arquivo deve ser incluído, False caso contrário
    """
    # Verifica extensão
    if file_path.suffix.lower() in ALLOWED_EXTENSIONS:
        return True
    
    # Verifica nome específico do arquivo
    if file_path.name in ALLOWED_FILENAMES:
        return True
    
    return False


def should_include_directory(dir_path: Path) -> bool:
    """
    Determina se um diretório deve ser incluído na varredura.
    
    Args:
        dir_path: Caminho do diretório
        
    Returns:
        True se o diretório deve ser incluído, False caso contrário
    """
    return dir_path.name not in IGNORED_DIRECTORIES


def categorize_file(file_path: Path) -> str:
    """
    Categoriza o tipo do arquivo.
    
    Args:
        file_path: Caminho do arquivo
        
    Returns:
        Categoria do arquivo
    """
    if file_path.suffix == '.java':
        return 'java_source'
    elif file_path.name == 'pom.xml':
        return 'maven_config'
    elif file_path.name.startswith('application'):
        return 'spring_config'
    elif file_path.suffix in ['.properties', '.yaml', '.yml']:
        return 'config'
    elif file_path.suffix == '.xml':
        return 'xml_config'
    else:
        return 'other'


def read_file_content(file_path: Path) -> Optional[str]:
    """
    Lê o conteúdo do arquivo com tratamento de diferentes encodings.
    
    Args:
        file_path: Caminho do arquivo
        
    Returns:
        Conteúdo do arquivo ou mensagem de erro
    """
    # Lista de encodings para tentar
    encodings = ['utf-8', 'latin-1', 'iso-8859-1', 'cp1252']
    
    for encoding in encodings:
        try:
            with open(file_path, 'r', encoding=encoding) as f:
                content = f.read()
                # Limita o tamanho do conteúdo se for muito grande
                max_size = 1024 * 1024  # 1MB
                if len(content) > max_size:
                    return content[:max_size] + f"\n\n[... conteúdo truncado - arquivo muito grande ({len(content)} bytes)]"
                return content
        except UnicodeDecodeError:
            continue
        except Exception as e:
            return f"[Erro ao ler arquivo: {str(e)}]"
    
    # Se nenhum encoding funcionou
    return "[Não foi possível ler o conteúdo do arquivo - encoding não suportado]"


def map_project_structure(root_path: Path) -> Dict:
    """
    Mapeia a estrutura do projeto, filtrando apenas artefatos Java e Spring Boot/Maven.
    
    Args:
        root_path: Caminho raiz do projeto
        
    Returns:
        Dicionário com a estrutura completa do projeto
    """
    # Verifica se é um projeto Spring Boot/Maven
    is_spring_maven = is_spring_boot_maven_project(root_path)
    
    structure = {
        "project_name": root_path.name,
        "timestamp": datetime.now().isoformat(),
        "root_path": str(root_path),
        "is_spring_boot_maven": is_spring_maven,
        "files": [],
        "directories": [],
        "statistics": {
            "total_files": 0,
            "total_directories": 0,
            "total_size": 0,
            "java_files": 0,
            "config_files": 0,
            "maven_files": 0
        }
    }
    
    if not is_spring_maven:
        structure["warning"] = "Projeto não identificado como Spring Boot/Maven. Nenhum arquivo foi mapeado."
        return structure
    
    def scan_directory(path: Path, relative_path: str = ""):
        """Escaneia recursivamente um diretório."""
        try:
            for item in path.iterdir():
                # Ignora diretórios que não devem ser incluídos
                if item.is_dir() and not should_include_directory(item):
                    continue
                
                rel_item_path = os.path.join(relative_path, item.name)
                
                if item.is_file():
                    # Filtra apenas arquivos relevantes
                    if should_include_file(item):
                        file_info = {
                            "name": item.name,
                            "path": rel_item_path,
                            "full_path": str(item),
                            "size": item.stat().st_size,
                            "extension": item.suffix,
                            "modified": datetime.fromtimestamp(item.stat().st_mtime).isoformat(),
                            "type": categorize_file(item),
                            "content": read_file_content(item)
                        }
                        structure["files"].append(file_info)
                        structure["statistics"]["total_files"] += 1
                        structure["statistics"]["total_size"] += file_info["size"]
                        
                        # Contadores por tipo
                        if item.suffix == '.java':
                            structure["statistics"]["java_files"] += 1
                        elif item.suffix in ['.properties', '.yaml', '.yml']:
                            structure["statistics"]["config_files"] += 1
                        elif item.suffix == '.xml':
                            structure["statistics"]["maven_files"] += 1
                    
                elif item.is_dir():
                    dir_info = {
                        "name": item.name,
                        "path": rel_item_path,
                        "full_path": str(item)
                    }
                    structure["directories"].append(dir_info)
                    structure["statistics"]["total_directories"] += 1
                    scan_directory(item, rel_item_path)
        except PermissionError:
            pass  # Ignora diretórios sem permissão
    
    scan_directory(root_path)
    return structure
