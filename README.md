# 🖥️ Sistema de Monitoramento
Este projeto é um sistema de monitoramento de hosts em tempo real. Ele coleta e exibe métricas importantes como  status dos serviços e conectividade, com interface visual e interativa.

🔧 Tecnologias Utilizadas
Java (Spring Boot) – Backend da aplicação com REST API e WebSocket.

JavaScript (ES6+) – Scripts interativos e dinâmicos no frontend.

HTML/CSS/SCSS – Interface estilizada e responsiva.

Chart.js – Geração de gráficos para visualização de métricas.

WebSocket – Atualização em tempo real das informações monitoradas.

⚙️ Funcionalidades
📡 Monitoramento em tempo real via WebSocket

📊 Visualização de métricas como CPU, memória e status dos hosts

🧠 Sistema de filtros por localização, IP e status

🔄 Atualização automática com loading em sobreposição suave

📈 Gráficos personalizáveis com cores e bordas transparentes

🖱️ Menu de contexto (botão direito) em cada host com ações rápidas

🌐 API REST para integração com outros sistemas

🚀 Como usar
Inicie o backend com Spring Boot

Acesse o frontend pelo navegador

Visualize os dados em tempo real através dos gráficos e tabelas

## 📘 Swagger Documentation

The API documentation is available via Swagger UI. To access it:

1. Start the application.
2. Navigate to `http://localhost:8083/swagger-ui.html`.
3. Use the `Authorization` header with a valid token to access protected endpoints.

### Example Token Usage
- Add `Authorization: Bearer <your-token>` in the request headers.

For more details, refer to the `SwaggerAccessFilter` implementation in the codebase.