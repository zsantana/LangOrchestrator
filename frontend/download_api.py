"""
API de Download de teste usando FastAPI.
Execute este script em um terminal separado para testar o download de arquivos.

Instale as dependências:
pip install fastapi uvicorn

Execute o servidor:
uvicorn download_api:app --reload --port 8000
"""

from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse, StreamingResponse
import os
from pathlib import Path
import zipfile
import io
from datetime import datetime

app = FastAPI(title="API de Download de Arquivos Processados")

# Diretório para arquivos processados simulados
PROCESSED_FILES_DIR = Path("processed_files")
PROCESSED_FILES_DIR.mkdir(exist_ok=True)


def create_sample_file(file_id: str) -> Path:
    """Cria um arquivo ZIP de exemplo para simular arquivo processado."""
    file_path = PROCESSED_FILES_DIR / f"{file_id}.zip"
    
    if not file_path.exists():
        # Criar conteúdo de exemplo
        with zipfile.ZipFile(file_path, 'w') as zipf:
            # Adicionar arquivo de resultado
            result_content = f"""
# Resultado do Processamento
File ID: {file_id}
Processado em: {datetime.now().isoformat()}
Status: Concluído com sucesso

## Estatísticas
- Arquivos analisados: 42
- Linhas de código: 1,337
- Tamanho total: 2.5 MB

## Arquivos principais
1. main.py
2. config.py
3. utils.py
4. README.md
"""
            zipf.writestr("result.txt", result_content)
            
            # Adicionar arquivo de metadados
            metadata = {
                "file_id": file_id,
                "processed_at": datetime.now().isoformat(),
                "status": "completed",
                "statistics": {
                    "files": 42,
                    "lines": 1337,
                    "size": "2.5 MB"
                }
            }
            
            import json
            zipf.writestr("metadata.json", json.dumps(metadata, indent=2))
    
    return file_path


@app.get("/")
async def root():
    """Endpoint raiz."""
    return {
        "message": "API de Download de Arquivos Processados",
        "endpoints": {
            "download": "/download/{file_id}",
            "list": "/files",
            "health": "/health"
        }
    }


@app.get("/health")
async def health_check():
    """Verifica o status da API."""
    return {"status": "healthy", "timestamp": datetime.now().isoformat()}


@app.get("/download/{file_id}")
async def download_file(file_id: str):
    """
    Baixa um arquivo processado pelo ID.
    
    Args:
        file_id: ID do arquivo a ser baixado
    
    Returns:
        FileResponse com o arquivo ZIP
    """
    try:
        # Criar arquivo de exemplo se não existir
        file_path = create_sample_file(file_id)
        
        if not file_path.exists():
            raise HTTPException(
                status_code=404,
                detail=f"Arquivo {file_id} não encontrado"
            )
        
        return FileResponse(
            path=file_path,
            filename=f"processed_{file_id}.zip",
            media_type="application/zip",
            headers={
                "Content-Disposition": f"attachment; filename=processed_{file_id}.zip"
            }
        )
    
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Erro ao processar download: {str(e)}"
        )


@app.get("/files")
async def list_files():
    """Lista todos os arquivos processados disponíveis."""
    files = []
    
    for file_path in PROCESSED_FILES_DIR.glob("*.zip"):
        stat = file_path.stat()
        files.append({
            "file_id": file_path.stem,
            "filename": file_path.name,
            "size": stat.st_size,
            "created_at": datetime.fromtimestamp(stat.st_ctime).isoformat(),
            "download_url": f"/download/{file_path.stem}"
        })
    
    return {
        "total": len(files),
        "files": files
    }


@app.delete("/files/{file_id}")
async def delete_file(file_id: str):
    """Remove um arquivo processado."""
    file_path = PROCESSED_FILES_DIR / f"{file_id}.zip"
    
    if not file_path.exists():
        raise HTTPException(
            status_code=404,
            detail=f"Arquivo {file_id} não encontrado"
        )
    
    try:
        file_path.unlink()
        return {"message": f"Arquivo {file_id} removido com sucesso"}
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Erro ao remover arquivo: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn
    print("🚀 Iniciando API de Download...")
    print("📡 URL: http://localhost:8000")
    print("📚 Documentação: http://localhost:8000/docs")
    uvicorn.run(app, host="0.0.0.0", port=8000)
