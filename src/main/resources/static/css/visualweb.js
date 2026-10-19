var socket = new SockJS('/ws');
var stompClient = Stomp.over(socket);
stompClient.debug = function() {};

var gruposConhecidos = new Set();
var filtroGrupoAtual = "";
var filtroStatusAtual = "";
var dadosAtuais = [];
var primeiroCarregamento = true;

document.getElementById("filtro-grupo").addEventListener("change", function () {
    filtroGrupoAtual = this.value;
    renderizar(dadosAtuais);
});

document.getElementById("online-count").parentElement.addEventListener("click", function () {
    filtroStatusAtual = filtroStatusAtual === "online" ? "" : "online";
    atualizarEstiloContadores();
    renderizar(dadosAtuais);
});

document.getElementById("offline-count").parentElement.addEventListener("click", function () {
    filtroStatusAtual = filtroStatusAtual === "offline" ? "" : "offline";
    atualizarEstiloContadores();
    renderizar(dadosAtuais);
});

document.getElementById("intermitente-count").parentElement.addEventListener("click", function () {
    filtroStatusAtual = filtroStatusAtual === "intermitente" ? "" : "intermitente";
    atualizarEstiloContadores();
    renderizar(dadosAtuais);
});

function atualizarEstiloContadores() {
    var boxes = document.querySelectorAll(".counter-box:not(.total-box)");
    boxes.forEach(function (box) { box.classList.remove("counter-active"); });

    if (filtroStatusAtual === "online") {
        document.getElementById("online-count").parentElement.classList.add("counter-active");
    } else if (filtroStatusAtual === "offline") {
        document.getElementById("offline-count").parentElement.classList.add("counter-active");
    } else if (filtroStatusAtual === "intermitente") {
        document.getElementById("intermitente-count").parentElement.classList.add("counter-active");
    }
}

function connectToWebSocket() {
    stompClient.connect({}, function (frame) {
        // Atualiza loading text
        var loadingText = document.getElementById("loading-container");
        if (loadingText) {
            loadingText.querySelector(".loading-text").innerText = "Aguardando dados...";
        }

        stompClient.subscribe('/lista', function (response) {
            var data = JSON.parse(response.body);
            dadosAtuais = data;
            marcarAtualizacao();

            // Esconde loading e mostra grid no primeiro carregamento
            if (primeiroCarregamento) {
                primeiroCarregamento = false;
                var loading = document.getElementById("loading-container");
                var grid = document.getElementById("meuContainer");
                if (loading) loading.style.display = "none";
                if (grid) grid.style.display = "";
            }

            // Atualiza lista de grupos no select
            var filtroSelect = document.getElementById("filtro-grupo");
            data.forEach(function (item) {
                if (item.grupo && !gruposConhecidos.has(item.grupo)) {
                    gruposConhecidos.add(item.grupo);
                    var option = document.createElement("option");
                    option.value = item.grupo;
                    option.textContent = item.grupo;
                    filtroSelect.appendChild(option);
                }
            });

            renderizar(data);
        });
    }, function(error) {
        var loadingText = document.querySelector(".loading-text");
        if (loadingText) loadingText.innerText = "Erro de conexao. Reconectando...";
        setTimeout(connectToWebSocket, 5000);
    });
}

function renderizar(data) {
    var container = document.getElementById("meuContainer");
    var totalCountEl = document.getElementById("total-count");
    var onlineCountEl = document.getElementById("online-count");
    var offlineCountEl = document.getElementById("offline-count");
    var intermitenteCountEl = document.getElementById("intermitente-count");

    var dataFiltrada = data;
    if (filtroGrupoAtual) {
        dataFiltrada = data.filter(function (item) {
            return item.grupo === filtroGrupoAtual;
        });
    }

    var onlineCount = 0;
    var offlineCount = 0;
    var intermitenteCount = 0;

    dataFiltrada.forEach(function (item) {
        var isOffline = (item.status || "").toUpperCase() === 'DOWN' || (item.status || "").toUpperCase() === 'OFFLINE';
        var isIntermitente = item.isIntermitencia === true;

        if (isOffline) {
            offlineCount++;
            if (isIntermitente) intermitenteCount++;
        } else if (isIntermitente) {
            intermitenteCount++;
            onlineCount++;
        } else {
            onlineCount++;
        }
    });

    if (totalCountEl) totalCountEl.innerText = dataFiltrada.length;
    if (onlineCountEl) onlineCountEl.innerText = onlineCount;
    if (offlineCountEl) offlineCountEl.innerText = offlineCount;
    if (intermitenteCountEl) intermitenteCountEl.innerText = intermitenteCount;

    if (filtroStatusAtual) {
        dataFiltrada = dataFiltrada.filter(function (item) {
            var isOffline = (item.status || "").toUpperCase() === 'DOWN' || (item.status || "").toUpperCase() === 'OFFLINE';
            var isIntermitente = item.isIntermitencia === true;

            if (filtroStatusAtual === "online") return !isOffline && !isIntermitente;
            if (filtroStatusAtual === "offline") return isOffline;
            if (filtroStatusAtual === "intermitente") return isIntermitente;
            return true;
        });
    }

    dataFiltrada.sort(function (a, b) {
        var isOffA = (a.status || "").toUpperCase() === 'DOWN' || (a.status || "").toUpperCase() === 'OFFLINE';
        var isOffB = (b.status || "").toUpperCase() === 'DOWN' || (b.status || "").toUpperCase() === 'OFFLINE';
        var isIntA = a.isIntermitencia === true;
        var isIntB = b.isIntermitencia === true;

        if (isOffA && !isOffB) return -1;
        if (!isOffA && isOffB) return 1;
        if (isIntA && !isIntB) return -1;
        if (!isIntA && isIntB) return 1;
        return 0;
    });

    var output = '';

    dataFiltrada.forEach(function (item) {
        var isOffline = (item.status || "").toUpperCase() === 'DOWN' || (item.status || "").toUpperCase() === 'OFFLINE';
        var isIntermitente = item.isIntermitencia === true;

        var pingBruto = item.pings || item.ping || "";
        var match = pingBruto.match(/Tempo:\s*([\d.]+)\s*ms/);
        var apenasTempo = match ? match[1] + " ms" : "---";

        var statusClass = 'dot-online';
        var statusLabel = 'ONLINE';
        var pulseColor = '#3b82f6';
        var cardClass = '';

        if (isOffline && isIntermitente) {
            statusClass = 'dot-offline';
            statusLabel = 'OFFLINE / INTERMITENTE';
            pulseColor = '#f1c40f';
            cardClass = 'card-offline card-warning';
        } else if (isOffline) {
            statusClass = 'dot-offline';
            statusLabel = 'OFFLINE';
            pulseColor = '#ef4444';
            cardClass = 'card-offline';
        } else if (isIntermitente) {
            statusClass = 'dot-warning';
            statusLabel = 'INTERMITENTE';
            pulseColor = '#f1c40f';
            cardClass = 'card-warning';
        }

        output += '<div class="card ' + cardClass + '">' +
            '<div class="card-header">' +
                '<div class="header-main">' +
                    '<span class="hostname">' + (item.localizacao || item.nome || '---') + '</span>' +
                    '<span class="ip-tag">' + (item.ip || '0.0.0.0') + '</span>' +
                '</div>' +
                '<svg class="card-detail-link" viewBox="0 0 24 24" fill="none" stroke="' + pulseColor + '" stroke-width="1.5" style="width:18px;height:20px;opacity:0.8;" title="Ver detalhes" onclick="abrirDetalhes(\'' + (item.localizacao || '').replace(/'/g, '') + '\', \'' + item.id + '\', \'' + (item.ip || '') + '\')">' +
                    '<path d="M22 12h-4l-3 9L9 3l-3 9H2"></path>' +
                '</svg>' +
            '</div>' +
            '<div class="card-body">' +
                '<div class="info-row">' +
                    '<span class="label">MAC:</span>' +
                    '<span class="value mac-style">' + (item.mac || '---') + '</span>' +
                '</div>' +
                '<div class="info-row">' +
                    '<span class="label">GRUPO:</span>' +
                    '<span class="value">' + (item.grupo || '---') + '</span>' +
                '</div>' +
            '</div>' +
            '<div class="card-footer">' +
                '<div class="status-indicator">' +
                    '<div class="dot ' + statusClass + '"></div>' +
                    '<span class="status-text">' + statusLabel + '</span>' +
                    '<span class="latencia-value">' + apenasTempo + '</span>' +
                '</div>' +
            '</div>' +
            '<div class="card-times">' +
                '<div class="info-row"><span class="label">ULT. ONLINE:</span><span class="value time-on">' + formatarData(item.dataUltimaVezOnline) + '</span></div>' +
                '<div class="info-row"><span class="label">ULT. OFFLINE:</span><span class="value time-off">' + formatarData(item.dataUltimaVezOffline) + '</span></div>' +
            '</div>' +
        '</div>';
    });

    if (container) container.innerHTML = output;
}

function abrirDetalhes(hostname, hostid, ip) {
    // Salvar no localStorage para persistencia
    localStorage.setItem('chart_hostid', hostid);
    localStorage.setItem('chart_hostname', hostname);
    localStorage.setItem('chart_interfaceIp', ip);
    window.open('/endpoint/char?hostname=' + encodeURIComponent(hostname) + '&interfaceIp=' + encodeURIComponent(ip) + '&hostid=' + hostid + '&itemid=1&limit=0');
}

function formatarData(data) {
    if (!data) return "--/-- --:--";
    // Se ja vem formatado "dd/MM/yyyy HH:mm:ss", mantem tudo
    if (typeof data === 'string' && data.includes('/')) {
        return data;
    }
    var date = new Date(data);
    var dia = String(date.getDate()).padStart(2,'0');
    var mes = String(date.getMonth()+1).padStart(2,'0');
    var ano = date.getFullYear();
    var hora = String(date.getHours()).padStart(2,'0');
    var min = String(date.getMinutes()).padStart(2,'0');
    var seg = String(date.getSeconds()).padStart(2,'0');
    return dia + '/' + mes + '/' + ano + ' ' + hora + ':' + min + ':' + seg;
}

function toggleFullscreen() {
    if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen();
    } else {
        document.exitFullscreen();
    }
}

connectToWebSocket();

function marcarAtualizacao() {
    var el = document.getElementById("ultima-atualizacao");
    if (el) el.innerText = "Ultima atualizacao: " + new Date().toLocaleTimeString("pt-BR");
}
