"""
Módulo para download de arquivos processados via API.
"""
from typing import Optional
import requests


class DownloadClient:
    """Cliente para download de arquivos processados."""
    
    def __init__(self, api_url: str):
        """
        Inicializa o cliente de download.
        
        Args:
            api_url: URL base da API de download
        """
        self.api_url = api_url
    
    def download_file(self, file_id: str, timeout: int = 30) -> tuple[bool, Optional[bytes], Optional[str]]:
        """
        Baixa arquivo processado via API.
        
        Args:
            file_id: ID do arquivo a baixar
            timeout: Timeout da requisição em segundos
            
        Returns:
            Tupla (sucesso, conteúdo, mensagem_erro)
        """
        try:
            response = requests.get(
                f"{self.api_url}/{file_id}",
                timeout=timeout
            )
            
            if response.status_code == 200:
                return True, response.content, None
            else:
                return False, None, f"Erro ao baixar arquivo: Status {response.status_code}"
                
        except requests.exceptions.Timeout:
            return False, None, "Timeout ao baixar arquivo"
        except requests.exceptions.ConnectionError:
            return False, None, "Erro de conexão com a API"
        except Exception as e:
            return False, None, f"Erro ao baixar arquivo: {str(e)}"
