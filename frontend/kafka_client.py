"""
Módulo para integração com Apache Kafka.
"""
import json
from typing import Dict

from kafka import KafkaProducer
from kafka.errors import KafkaError


class KafkaClient:
    """Cliente para envio de mensagens ao Kafka."""
    
    def __init__(self, bootstrap_servers: str):
        """
        Inicializa o cliente Kafka.
        
        Args:
            bootstrap_servers: Endereço dos servidores Kafka
        """
        self.bootstrap_servers = bootstrap_servers
        self.producer = None
    
    def send_message(self, data: Dict, topic: str) -> tuple[bool, str]:
        """
        Envia dados JSON para o tópico Kafka.
        
        Args:
            data: Dicionário com dados a enviar
            topic: Nome do tópico Kafka
            
        Returns:
            Tupla (sucesso, mensagem)
        """
        try:
            producer = KafkaProducer(
                bootstrap_servers=self.bootstrap_servers,
                value_serializer=lambda v: json.dumps(v).encode('utf-8'),
                acks='all',
                retries=3
            )
            
            future = producer.send(topic, value=data)
            record_metadata = future.get(timeout=10)
            
            producer.flush()
            producer.close()
            
            return True, f"Mensagem enviada com sucesso para {topic}"
            
        except KafkaError as e:
            return False, f"Erro ao enviar para Kafka: {str(e)}"
        except Exception as e:
            return False, f"Erro inesperado: {str(e)}"
    
    def __del__(self):
        """Garante que o producer seja fechado corretamente."""
        if self.producer:
            try:
                self.producer.close()
            except:
                pass
