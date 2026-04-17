var socket = new SockJS('/ws');
var stompClient = Stomp.over(socket);
stompClient.debug = () => {}

function connectToWebSocket() {
    stompClient.connect({}, function (frame) {
        stompClient.subscribe('/lista', function (response) {

            var data = JSON.parse(response.body);
            let container = document.getElementById("meuContainer");
            let onlineCountEl = document.getElementById("online-count");
            let offlineCountEl = document.getElementById("offline-count");

            let output = '';
            let onlineCount = 0;
            let offlineCount = 0;

            // Ordenação: Problemas (Offline) primeiro
            data.sort((a, b) => (a.status === 'DOWN' || a.status === 'OFFLINE' ? -1 : 1));
data.forEach(function (item) {
    const isOffline = (item.status || "").toUpperCase() === 'DOWN' || (item.status || "").toUpperCase() === 'OFFLINE';
    // Verifica se o item é intermitente (aceita boolean true ou 1 do banco)
    const isIntermitente = item.isIntermitencia === 1 || item.isIntermitencia === true;
     const pingBruto = item.ping || "";

         // 2. Usa Regex para extrair apenas os números decimais que vêm após "Tempo:" ou "time="
         // O [0] pega a primeira ocorrência, ou você pode usar matchAll para todas
         const match = pingBruto.match(/(?:Tempo:|time=)\s*([\d.]+)/);

         // 3. Se encontrar, armazena o valor (ex: "0.171"), senão coloca "---"
         const apenasTempo = match ? match[1] + " ms" : "---";
    isOffline ? offlineCount++ : onlineCount++;

    // Lógica de definição de cores e rótulos
    let statusClass = 'dot-online';
    let statusLabel = 'ONLINE';
    let pulseColor = '#3b82f6'; // Azul padrão

    if (isOffline) {
        statusClass = 'dot-offline';
        statusLabel = 'OFFLINE';
        pulseColor = '#ef4444'; // Vermelho
    } else if (isIntermitente) {
        statusClass = 'dot-warning'; // Amarelo
        statusLabel = 'INTERMITENTE';
        pulseColor = '#f1c40f'; // Amarelo para o pulso
    }

    output += `
        <div class="card ${isOffline ? 'card-offline' : (isIntermitente ? 'card-warning' : '')}">
            <div class="card-header">
                <div class="header-main">
                    <span class="hostname">${item.localizacao}</span>
                    <span class="ip-tag">${item.ip || '0.0.0.0'}</span>
                </div>
                <svg class="chart-icon"
                     viewBox="0 0 24 24"
                     fill="none"
                     stroke="${pulseColor}"
                     stroke-width="1.5"
                     style="width: 18px; height: 22px; opacity: 0.8;">
                    <path d="M22 12h-4l-3 9L9 3l-3 9H2"></path>
                </svg>
            </div>

            <div class="card-body">
                <div class="info-row">
                    <span class="label">MAC:</span>
                    <span class="value mac-style">${item.mac || '---'}</span>
                </div>
                <div class="info-row">
                    <span class="label">GRUPO:</span>
                    <span class="value">${item.grupo}</span>
                </div>
            </div>

            <div class="card-footer">
                <div class="status-indicator">
                    <div class="dot ${statusClass}"></div>
                    <span class="status-text">${statusLabel}</span>
                    <span >${apenasTempo}</span>
                </div>
                <span class="timestamp">${formatarData(item.checkOut)}</span>
            </div>
        </div>
    `;
});

            if (container) container.innerHTML = output;
            if (onlineCountEl) onlineCountEl.innerText = onlineCount;
            if (offlineCountEl) offlineCountEl.innerText = offlineCount;
        });
    }, function(error) {
        setTimeout(connectToWebSocket, 5000);
    });
}

function formatarData(data) {
    if (!data) return "--:--";
    let date = new Date(data);
    return date.toLocaleTimeString('pt-BR', {hour: '2-digit', minute:'2-digit', second:'2-digit'});
}

connectToWebSocket();