var socket = new SockJS('/ws');
var stompClient = Stomp.over(socket);
stompClient.debug = () => { };
const endpoint = "http://10.220.112.112:8083";

function connectToWebSocket() {
    let clientId = 'client-' + Date.now().toString(36);
    localStorage.setItem("client", clientId);

    stompClient.connect({}, function (frame) {
        let loading = document.getElementById("overlay-loader");
        let grid = document.getElementById("device-grid");
        let summary = document.getElementById("summary-counters");

        stompClient.subscribe(`/topic/monitor/${clientId}`, function (response) {
            var data = JSON.parse(response.body);
            let htmlContent = '';
            let countUp = 0;
            let countDown = 0;

            if (!data || data.length === 0) {
                loading.style.display = "none";
                document.getElementById("overlay").innerHTML = "Grupo vazio ou inexistente.";
                return;
            }

            loading.style.display = "none";
            document.getElementById("overlay").style.display = "none";
            summary.style.display = "flex";

            // Ordena para mostrar os OFFLINE primeiro (Prioridade de atenção)
            data.sort((a, b) => a.status === 'UP' ? 1 : -1);

            data.forEach(function (item) {
                const isUp = item.status.toUpperCase() === 'UP';
                isUp ? countUp++ : countDown++;

                htmlContent += `
                    <div class="area1 ${isUp ? 'status-up-card' : 'status-down-card'}">
                        <div class="area-header">
                            <div>
                                <p id="profile">${item.localizacao}</p>
                                <span class="ip-box">${item.ip}</span>
                            </div>
                            <span class="material-symbols-outlined" 
                                  style="cursor:pointer; color:var(--primary); font-size: 20px;" 
                                  onclick="latencia('${item.localizacao}','${item.id}','1','${item.ip}')">
                                  monitoring
                            </span>
                        </div>
                        <div style="display:flex; justify-content: space-between; align-items: center; margin-top: 10px;">
                            <span style="font-size: 10px; color: #8b949e;">
                                ${isUp && item.dataUltimaVezOnline  ? 'Verificado: ' + item.dataUltimaVezOnline : 'Ultimo Visto '+item.dataUltimaVezOnline}
                            </span>
                            <span style="font-weight: bold; font-size: 11px; color: ${isUp ? '#39d353' : '#f85149'}">
                                ${isUp ? '● ONLINE' : '● OFFLINE'}
                            </span>
                        </div>
                    </div>
                `;
            });

            document.getElementById("count-up-total").innerText = countUp;
            document.getElementById("count-down-total").innerText = countDown;
            grid.innerHTML = htmlContent;
        });

        setInterval(buscarMacLocal, 1000);
    });
}

function buscarMac() {
    const val = document.getElementById("stc").value;
    const client = localStorage.getItem("client");
    if (val) {
        localStorage.setItem("ReceiveGrupoId", val);
        stompClient.send(`/app/mestricasM/${client}`, {}, JSON.stringify({clientId: client, ReceiveGrupoId: val}));
    }
}

function buscarMacLocal() {
    const client = localStorage.getItem("client");
    const item = localStorage.getItem("ReceiveGrupoId");
    const titule = document.getElementById("Monitoramento");
    if (item) {
        titule.innerHTML = "Monitorando: " + item;
        stompClient.send("/app/mestricasM", {}, JSON.stringify({clientId: client, ReceiveGrupoId: item}));
    } else {
        document.getElementById("overlay").innerHTML = "Aguardando seleção de grupo...";
        document.getElementById("overlay-loader").style.display = "none";
    }
}

async function Onlw() {
    const statusDiv = document.getElementById("status");
    try {
        const resp = await fetch(`${endpoint}/endpoint/monitoramento`);
        statusDiv.innerHTML = resp.status === 200 ? "● SISTEMA OK" : "● ERRO API";
        statusDiv.className = resp.status === 200 ? "online" : "offline";
    } catch (e) {
        statusDiv.innerHTML = "● OFFLINE";
        statusDiv.className = "offline";
    }
}

function latencia(loc, hostid, itemid, ip) {
    window.open(`${endpoint}/endpoint/char?hostname=${loc}&interfaceIp=${ip}&hostid=${hostid}&itemid=${itemid}&limit=0`);
}

async function itensICMP() {
    const resp = await fetch(`${endpoint}/itens/group`);
    const itens = await resp.json();
    let option = '<option value="">Selecionar Grupo...</option>';
    itens.forEach(e => option += `<option value="${e.nome}">${e.nome}</option>`);
    document.getElementById("stc").innerHTML = option;
}

window.addEventListener("DOMContentLoaded", () => {
    itensICMP();
    connectToWebSocket();
    setInterval(Onlw, 2000);
});