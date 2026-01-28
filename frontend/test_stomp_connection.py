#!/usr/bin/env python3
"""
Script de teste para validar conexão WebSocket/STOMP com Spring Boot.

Uso:
    python test_stomp_connection.py

Este script:
1. Conecta ao servidor via ws:// (WebSocket)
2. Implementa protocolo STOMP sobre WebSocket
3. Inscreve no canal geral /topic/notifications
4. Opcionalmente inscreve em um projeto específico
5. Escuta por notificações em tempo real
"""

import websocket
import json
import sys
import time
from datetime import datetime


class STOMPWebSocketClient:
    """Cliente WebSocket com suporte a STOMP."""
    
    def __init__(self, url):
        self.url = url
        self.ws = None
        self.subscription_counter = 0
    
    def _send_stomp_frame(self, command, headers=None, body=""):
        """Envia um frame STOMP."""
        frame = f"{command}\n"
        if headers:
            for key, value in headers.items():
                frame += f"{key}:{value}\n"
        frame += "\n" + body + "\x00"
        
        self.ws.send(frame)
    
    def _parse_stomp_frame(self, data):
        """Parse de um frame STOMP."""
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
        """Callback de mensagem."""
        frame = self._parse_stomp_frame(message)
        if not frame:
            return
        
        if frame['command'] == 'CONNECTED':
            print("\n✅ STOMP CONNECTED")
            print(f"   Session: {frame['headers'].get('session', 'N/A')}")
            print(f"   Server: {frame['headers'].get('server', 'N/A')}")
            print("-" * 60)
            
            # Inscrever no canal geral
            self._send_stomp_frame('SUBSCRIBE', {
                'id': 'sub-0',
                'destination': '/topic/notifications'
            })
            print("\n📡 Inscrito em: /topic/notifications")
            
        elif frame['command'] == 'MESSAGE':
            try:
                notification = json.loads(frame['body'])
                channel = frame['headers'].get('destination', 'unknown')
                
                timestamp = datetime.now().strftime('%H:%M:%S')
                notif_type = notification.get('type', 'UNKNOWN')
                project_id = notification.get('projectId', 'N/A')
                message_text = notification.get('message', '')
                
                print(f"\n📨 [{timestamp}] Nova Notificação")
                print(f"   Canal: {channel}")
                print(f"   Tipo: {notif_type}")
                print(f"   Projeto: {project_id}")
                print(f"   Mensagem: {message_text}")
                
                if 'data' in notification:
                    data = notification['data']
                    print(f"   Dados:")
                    for key, value in data.items():
                        print(f"      - {key}: {value}")
                
                print("-" * 60)
                
            except json.JSONDecodeError as e:
                print(f"❌ Erro ao decodificar JSON: {e}")
        
        elif frame['command'] == 'ERROR':
            print(f"\n❌ STOMP ERROR: {frame['body']}")
    
    def on_error(self, ws, error):
        """Callback de erro."""
        print(f"\n❌ WebSocket Error: {error}")
    
    def on_close(self, ws, close_status_code, close_msg):
        """Callback de fechamento."""
        print(f"\n⚠️  Conexão fechada: {close_status_code} - {close_msg}")
    
    def on_open(self, ws):
        """Callback de abertura."""
        print("\n✅ WebSocket Conectado!")
        print("🔄 Enviando STOMP CONNECT...")
        
        # Enviar CONNECT STOMP
        self._send_stomp_frame('CONNECT', {
            'accept-version': '1.0,1.1,1.2',
            'heart-beat': '10000,10000'
        })
    
    def subscribe_to_project(self, project_id):
        """Inscrever em projeto específico."""
        self.subscription_counter += 1
        topic = f'/topic/project/{project_id}'
        
        self._send_stomp_frame('SUBSCRIBE', {
            'id': f'sub-{self.subscription_counter}',
            'destination': topic
        })
        
        print(f"\n📡 Inscrito em: {topic}")
    
    def connect(self, project_id=None):
        """Conectar ao WebSocket."""
        self.ws = websocket.WebSocketApp(
            self.url,
            on_open=self.on_open,
            on_message=self.on_message,
            on_error=self.on_error,
            on_close=self.on_close
        )
        
        # Agendar inscrição em projeto se fornecido
        if project_id:
            def subscribe_later():
                time.sleep(2)
                self.subscribe_to_project(project_id)
            
            import threading
            threading.Thread(target=subscribe_later, daemon=True).start()
        
        # Executar forever
        self.ws.run_forever()


def main():
    """Função principal."""
    
    # Configurações
    WEBSOCKET_URL = 'ws://localhost:8080/ws-native'
    
    print("=" * 60)
    print("🔌 Teste de Conexão WebSocket/STOMP - Spring Boot")
    print("=" * 60)
    print(f"\nConfigurações:")
    print(f"   URL: {WEBSOCKET_URL}")
    print(f"   Protocolo: WebSocket (ws://) com STOMP")
    print(f"   Canal Geral: /topic/notifications")
    
    # Pergunta se quer inscrever em projeto específico
    print("\n" + "=" * 60)
    project_id = input("ID do Projeto (Enter para apenas canal geral): ").strip()
    
    # Criar cliente
    print("\n🔄 Conectando ao servidor WebSocket...")
    client = STOMPWebSocketClient(WEBSOCKET_URL)
    
    try:
        # Conectar (bloqueante)
        print("\n" + "=" * 60)
        print("👂 Escutando notificações...")
        print("   Pressione Ctrl+C para sair")
        print("=" * 60)
        
        client.connect(project_id if project_id else None)
    
    except KeyboardInterrupt:
        print("\n\n🛑 Encerrando...")
    
    except Exception as e:
        print(f"\n❌ Erro: {e}")
        import traceback
        traceback.print_exc()
    
    finally:
        print("✅ Finalizado!\n")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"\n❌ Erro fatal: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
