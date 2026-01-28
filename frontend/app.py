"""
Sistema de Processamento de Projetos - Interface Streamlit
"""
import streamlit as st
import zipfile
import json
import time
from pathlib import Path
from datetime import datetime
import pandas as pd

# Imports dos módulos criados
from config import (
    KAFKA_BOOTSTRAP_SERVERS,
    KAFKA_TOPIC,
    WEBSOCKET_URL,
    WEBSOCKET_TOPIC_PROJECT,
    API_DOWNLOAD_URL,
    UPLOAD_DIR,
    EXTRACT_DIR,
    PROCESSED_DIR,
    ensure_directories
)
from project_analyzer import map_project_structure
from kafka_client import KafkaClient
from websocket_client import WebSocketClient
from download_client import DownloadClient

# Criar diretórios necessários
ensure_directories()

# Criar diretórios necessários
ensure_directories()

# Inicializar session state
if 'processing_status' not in st.session_state:
    st.session_state.processing_status = []
if 'notifications' not in st.session_state:
    st.session_state.notifications = []
if 'ws_connected' not in st.session_state:
    st.session_state.ws_connected = False
if 'notification_count' not in st.session_state:
    st.session_state.notification_count = 0
if 'auto_refresh' not in st.session_state:
    st.session_state.auto_refresh = True

def sync_websocket_state():
    """Sincroniza estado do WebSocket com session_state."""
    if 'ws_client' in st.session_state:
        st.session_state.ws_connected = st.session_state.ws_client.connected
        
        # Obter novas notificações
        new_notifications = st.session_state.ws_client.get_notifications()
        has_new = len(new_notifications) > 0
        
        # Adicionar novas notificações ao session_state
        for notification in new_notifications:
            if notification not in st.session_state.notifications:
                st.session_state.notifications.append(notification)
                st.session_state.notification_count += 1
        
        # Mostrar erro se houver
        error = st.session_state.ws_client.get_error()
        if error:
            st.error(error)
        
        return has_new
    return False


# Interface Streamlit
st.set_page_config(
    page_title="Processador de Projetos",
    page_icon="📦",
    layout="wide"
)

st.title("📦 Sistema de Processamento de Projetos")
st.markdown("---")

# Sidebar para configurações
with st.sidebar:
    st.header("⚙️ Configurações")
    
    st.subheader("Kafka")
    kafka_servers = st.text_input("Bootstrap Servers", KAFKA_BOOTSTRAP_SERVERS)
    kafka_topic = st.text_input("Tópico", KAFKA_TOPIC)
    
    st.subheader("WebSocket (STOMP)")
    websocket_url = st.text_input("URL WebSocket", WEBSOCKET_URL)
    st.caption("Exemplo: ws://localhost:8080/ws-native (sem SockJS)")
    
    col1, col2 = st.columns(2)
    with col1:
        if st.button("🔌 Conectar"):
            if 'ws_client' not in st.session_state:
                st.session_state.ws_client = WebSocketClient(websocket_url)
            if st.session_state.ws_client.connect():
                sync_websocket_state()
                st.success("Conectado!")
            else:
                sync_websocket_state()
                st.error("Falha na conexão")
    
    with col2:
        if st.button("❌ Desconectar"):
            if 'ws_client' in st.session_state:
                st.session_state.ws_client.disconnect()
                sync_websocket_state()
                st.info("Desconectado")
    
    # Sincronizar estado continuamente
    has_new = sync_websocket_state()
    
    # Auto-refresh se habilitado e há novas notificações
    if has_new and st.session_state.auto_refresh:
        time.sleep(0.5)
        st.rerun()
    
    if st.session_state.ws_connected:
        st.success("✅ Conectado")
        
        # Opção para inscrever em projeto específico
        project_id_input = st.text_input("ID do Projeto (opcional)")
        if st.button("📡 Inscrever no Projeto") and project_id_input:
            if 'ws_client' in st.session_state:
                if st.session_state.ws_client.subscribe_to_project(project_id_input):
                    st.success(f"Inscrito em: /topic/project/{project_id_input}")
    else:
        st.warning("⚠️ Desconectado")
    
    st.markdown("---")
    st.subheader("API Download")
    api_url = st.text_input("URL da API", API_DOWNLOAD_URL)

# Tabs principais
tab1, tab2, tab3 = st.tabs(["📤 Upload e Processamento", "📊 Painel de Acompanhamento", "🔔 Notificações"])

with tab1:
    st.header("Upload de Projeto")
    
    uploaded_file = st.file_uploader(
        "Selecione o arquivo ZIP do projeto",
        type=['zip'],
        help="Faça upload de um arquivo ZIP contendo o projeto a ser processado"
    )
    
    if uploaded_file is not None:
        col1, col2 = st.columns([3, 1])
        
        with col1:
            st.info(f"📁 Arquivo: {uploaded_file.name} ({uploaded_file.size / 1024:.2f} KB)")
        
        with col2:
            process_button = st.button("🚀 Processar", type="primary", use_container_width=True)
        
        if process_button:
            with st.spinner("Processando projeto..."):
                try:
                    # 1. Salvar arquivo ZIP
                    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                    project_id = f"project_{timestamp}"
                    zip_path = UPLOAD_DIR / f"{project_id}.zip"
                    
                    with open(zip_path, "wb") as f:
                        f.write(uploaded_file.getbuffer())
                    
                    st.success(f"✅ Arquivo salvo: {zip_path.name}")
                    
                    # 2. Descompactar
                    extract_path = EXTRACT_DIR / project_id
                    extract_path.mkdir(exist_ok=True)
                    
                    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
                        zip_ref.extractall(extract_path)
                    
                    st.success(f"✅ Projeto descompactado em: {extract_path.name}")
                    
                    # 3. Mapear estrutura
                    project_structure = map_project_structure(extract_path)
                    project_structure['project_id'] = project_id
                    
                    st.success("✅ Estrutura do projeto mapeada")
                    
                    # Exibir preview da estrutura
                    with st.expander("🔍 Preview da Estrutura JSON"):
                        st.json(project_structure)
                    
                    # 4. Enviar para Kafka
                    kafka_client = KafkaClient(kafka_servers)
                    success, message = kafka_client.send_message(project_structure, kafka_topic)
                    
                    if success:
                        st.success(f"✅ {message}")
                        
                        # Adicionar ao status de processamento
                        status_entry = {
                            "project_id": project_id,
                            "filename": uploaded_file.name,
                            "uploaded_at": datetime.now().isoformat(),
                            "status": "Enviado para processamento",
                            "files_count": project_structure['statistics']['total_files'],
                            "total_size": project_structure['statistics']['total_size']
                        }
                        st.session_state.processing_status.append(status_entry)
                        
                        # Salvar JSON localmente
                        json_path = PROCESSED_DIR / f"{project_id}.json"
                        with open(json_path, 'w', encoding='utf-8') as f:
                            json.dump(project_structure, f, indent=2, ensure_ascii=False)
                        
                        st.success("🎉 Processamento concluído com sucesso!")
                    else:
                        st.error(f"❌ {message}")
                        
                except Exception as e:
                    st.error(f"❌ Erro no processamento: {str(e)}")

with tab2:
    st.header("📊 Painel de Acompanhamento")
    
    if st.session_state.processing_status:
        # Métricas gerais
        col1, col2, col3 = st.columns(3)
        
        with col1:
            st.metric("Total de Projetos", len(st.session_state.processing_status))
        
        with col2:
            total_files = sum(p['files_count'] for p in st.session_state.processing_status)
            st.metric("Total de Arquivos", total_files)
        
        with col3:
            total_size = sum(p['total_size'] for p in st.session_state.processing_status)
            st.metric("Tamanho Total", f"{total_size / (1024*1024):.2f} MB")
        
        st.markdown("---")
        
        # Tabela de status
        df = pd.DataFrame(st.session_state.processing_status)
        df['uploaded_at'] = pd.to_datetime(df['uploaded_at']).dt.strftime('%Y-%m-%d %H:%M:%S')
        df['total_size_mb'] = (df['total_size'] / (1024*1024)).round(2)
        
        st.dataframe(
            df[['project_id', 'filename', 'uploaded_at', 'status', 'files_count', 'total_size_mb']],
            use_container_width=True,
            column_config={
                "project_id": "ID do Projeto",
                "filename": "Arquivo",
                "uploaded_at": "Data de Upload",
                "status": "Status",
                "files_count": "Nº Arquivos",
                "total_size_mb": "Tamanho (MB)"
            }
        )
        
        if st.button("🗑️ Limpar Histórico"):
            st.session_state.processing_status = []
            st.rerun()
    else:
        st.info("Nenhum projeto processado ainda.")

with tab3:
    st.header("🔔 Notificações WebSocket")
    
    col1, col2, col3 = st.columns([2, 1, 1])
    
    with col1:
        st.session_state.auto_refresh = st.toggle(
            "🔄 Atualização Automática", 
            value=st.session_state.auto_refresh,
            help="Atualiza automaticamente quando novas notificações chegam"
        )
    
    with col2:
        if st.button("🔄 Atualizar Agora", use_container_width=True):
            st.rerun()
    
    with col3:
        if st.button("🗑️ Limpar", use_container_width=True):
            st.session_state.notifications = []
            st.session_state.notification_count = 0
            st.rerun()
    
    # Auto-refresh: verifica periodicamente se há novas notificações
    if st.session_state.auto_refresh and st.session_state.ws_connected:
        if 'ws_client' in st.session_state:
            # Criar um placeholder para forçar atualização
            placeholder = st.empty()
            with placeholder.container():
                st.caption(f"🔄 Monitorando... (Total: {len(st.session_state.notifications)})")
            
            # Pequeno delay e rerun para continuar monitorando
            time.sleep(2)
            st.rerun()
    
    if st.session_state.notifications:
        st.success(f"Total de notificações: {len(st.session_state.notifications)}")
        
        for idx, notification in enumerate(reversed(st.session_state.notifications)):
            with st.expander(
                f"📬 Notificação #{len(st.session_state.notifications) - idx} - {notification.get('received_at', 'N/A')}"
            ):
                col1, col2 = st.columns([3, 1])
                
                with col1:
                    st.json(notification)
                
                with col2:
                    if 'file_id' in notification:
                        if st.button(
                            "⬇️ Download",
                            key=f"download_{idx}",
                            use_container_width=True
                        ):
                            download_client = DownloadClient(api_url)
                            success, file_content, error = download_client.download_file(notification['file_id'])
                            
                            if success:
                                st.download_button(
                                    label="💾 Salvar Arquivo",
                                    data=file_content,
                                    file_name=f"processed_{notification['file_id']}.zip",
                                    mime="application/zip",
                                    key=f"save_{idx}"
                                )
                            else:
                                st.error(error)
    else:
        st.info("Nenhuma notificação recebida ainda. Conecte-se ao WebSocket para receber atualizações.")

# Footer
st.markdown("---")
st.markdown(
    """
    <div style='text-align: center; color: #666;'>
    Sistema de Processamento de Projetos v1.0 | 
    Kafka Producer | WebSocket Client | Download API
    </div>
    """,
    unsafe_allow_html=True
)
