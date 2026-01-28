"""
Módulo para conexão WebSocket com STOMP.
"""
import json
import time
import threading
from typing import Dict, List, Optional
from datetime import datetime

import websocket


class WebSocketClient:
    """
    Cliente WebSocket com STOMP para receber notificações de processamento.
    Conecta via ws:// ao Spring Boot WebSocket endpoint.
    """
    
    def __init__(self, url: str):
        """
        Inicializa o cliente WebSocket.
        
        Args:
            url: URL do endpoint WebSocket
        """
        self.url = url
        self.ws = None
        self.running = False
        self.thread = None
        self.subscribed_topics = []
        self.subscription_counter = 0
        self.connected = False
        self.notifications = []
        self.error_message = None
    
    def _send_stomp_frame(self, command: str, headers: Dict = None, body: str = "") -> bool:
        """
        Envia um frame STOMP pelo WebSocket.
        
        Args:
            command: Comando STOMP
            headers: Cabeçalhos do frame
            body: Corpo da mensagem
            
        Returns:
            True se enviado com sucesso, False caso contrário
        """
        if not self.ws:
            return False
        
        frame = f"{command}\n"
        if headers:
            for key, value in headers.items():
                frame += f"{key}:{value}\n"
        frame += "\n" + body + "\x00"
        
        try:
            self.ws.send(frame)
            return True
        except Exception as e:
            print(f"Erro ao enviar frame STOMP: {e}")
            return False
    
    def _parse_stomp_frame(self, data: str) -> Optional[Dict]:
        """
        Parse de um frame STOMP.
        
        Args:
            data: Dados recebidos
            
        Returns:
            Dicionário com command, headers e body
        """
        if not data or data == '\n':
            return None
        
        lines = data.split('\n')
        command = lines[0] if lines else ""
        
        headers = {}
        body_start = 1
        for i in range(1, len(lines)):
            if lines[i] == '':
                body_start = i + 1
                break
            if ':' in lines[i]:
                key, value = lines[i].split(':', 1)
                headers[key] = value
        
        body = '\n'.join(lines[body_start:]).rstrip('\x00')
        
        return {'command': command, 'headers': headers, 'body': body}
    
    def on_message(self, ws, message):
        """Callback quando recebe mensagem WebSocket."""
        try:
            frame = self._parse_stomp_frame(message)
            if not frame:
                return
            
            if frame['command'] == 'CONNECTED':
                self.connected = True
                # Inscrever automaticamente no canal geral
                self._subscribe_internal('/topic/notifications')
                
            elif frame['command'] == 'MESSAGE':
                # Processar notificação
                notification = json.loads(frame['body'])
                notification['received_at'] = datetime.now().isoformat()
                notification['channel'] = frame['headers'].get('destination', 'unknown')
                self.notifications.append(notification)
                
            elif frame['command'] == 'ERROR':
                self.error_message = f"STOMP Error: {frame['body']}"
                self.connected = False
                
        except Exception as e:
            print(f"Erro ao processar mensagem: {e}")
    
    def on_error(self, ws, error):
        """Callback de erro."""
        self.error_message = str(error)
        self.connected = False
    
    def on_close(self, ws, close_status_code, close_msg):
        """Callback ao fechar conexão."""
        self.connected = False
        self.running = False
    
    def on_open(self, ws):
        """Callback ao abrir conexão."""
        # Enviar frame CONNECT do STOMP
        self._send_stomp_frame('CONNECT', {
            'accept-version': '1.0,1.1,1.2',
            'heart-beat': '10000,10000'
        })
    
    def _subscribe_internal(self, topic: str) -> bool:
        """
        Método interno para inscrever em tópico.
        
        Args:
            topic: Nome do tópico
            
        Returns:
            True se inscrito com sucesso
        """
        subscription_id = f"sub-{self.subscription_counter}"
        self.subscription_counter += 1
        
        self._send_stomp_frame('SUBSCRIBE', {
            'id': subscription_id,
            'destination': topic
        })
        
        self.subscribed_topics.append(topic)
        return True
    
    def connect(self) -> bool:
        """
        Inicia conexão WebSocket.
        
        Returns:
            True se conectado com sucesso
        """
        try:
            def run():
                self.ws = websocket.WebSocketApp(
                    self.url,
                    on_open=self.on_open,
                    on_message=self.on_message,
                    on_error=self.on_error,
                    on_close=self.on_close
                )
                self.running = True
                self.ws.run_forever(reconnect=5)
            
            self.thread = threading.Thread(target=run, daemon=True)
            self.thread.start()
            
            # Aguardar conexão
            time.sleep(2)
            return self.connected
            
        except Exception as e:
            self.error_message = f"Erro ao conectar WebSocket: {str(e)}"
            return False
    
    def subscribe(self, topic: str) -> bool:
        """
        Inscreve em um tópico específico.
        
        Args:
            topic: Nome do tópico
            
        Returns:
            True se inscrito com sucesso
        """
        if self.ws and self.connected:
            return self._subscribe_internal(topic)
        return False
    
    def subscribe_to_project(self, project_id: str) -> bool:
        """
        Inscreve no canal específico de um projeto.
        
        Args:
            project_id: ID do projeto
            
        Returns:
            True se inscrito com sucesso
        """
        topic = f"/topic/project/{project_id}"
        return self.subscribe(topic)
    
    def disconnect(self):
        """Fecha conexão WebSocket."""
        if self.ws:
            self._send_stomp_frame('DISCONNECT')
            self.ws.close()
            self.running = False
        self.connected = False
    
    def get_notifications(self) -> List[Dict]:
        """
        Retorna e limpa lista de notificações.
        
        Returns:
            Lista de notificações recebidas
        """
        notifications = self.notifications.copy()
        self.notifications.clear()
        return notifications
    
    def get_error(self) -> Optional[str]:
        """
        Retorna e limpa mensagem de erro.
        
        Returns:
            Mensagem de erro ou None
        """
        error = self.error_message
        self.error_message = None
        return error
