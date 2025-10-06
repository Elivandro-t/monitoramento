var socket = new SockJS('/ws');  // Ponto de entrada do WebSocket
var stompClient = Stomp.over(socket);
let indexs;
stompClient.debug = () => { };
const endpoint = "http://192.168.88.70:8080";

function connectToWebSocket() {
    let clientId = 'client-' + Date.now().toString(36) + '-' + Math.random().toString(36).substr(2, 5);
    localStorage.setItem("client", clientId);
    const client = localStorage.getItem("client");

    stompClient.connect({}, function (frame) {
        let loading = document.getElementById("overlay-loader");
        stompClient.subscribe(`/topic/monitor/${client}`, function (response) {
            var data = JSON.parse(response.body);
            let downContent = '';
            let upContent = '';
            let countDown = 0;
            let countUp = 0;
            if (data.length < 0) {
                loading.style.display = "block";
                return;
            }
            else if (data.length === 0) {
                loading.style.display = "block";
                loading.style.innerHTML = "Sem itens!";
            }
            loading.style.display = "none";
            data.forEach(function (item, index) {
                const status = item.status.toUpperCase();
                indexs = index;
                const content = `
                    <div class="area1">
                        <div class="area-header">
                             <div class="log_ip ms">
                                <p id="profile">${item.localizacao}</p>

                            <a id="data" class="data">
                                <span class="material-symbols-outlined">
                                    error
                                </span>
                            </a>
                            <section class="data_ip exibir" id="exibir ">${item.ip}</section>
                            </div>
                            <a onclick="latencia('${item.localizacao}','${item.id}','1','${item.ip}')"><span class="material-symbols-outlined">
                                open_in_new
                                </span></a>
                        </div>
                        <div class="area-header">
                             <p id="profile">${item.dataUltimaVezOnline !== null ? "Ultimo visto: " + item.dataUltimaVezOnline : ""}</p>
                        </div>
                        <div class="gauge-container">
                            <div class="gauge-arc ${status === 'UP' ? 'percent-label' : 'percent-labelRED'}">
                                <div class="gauge-fill" style="width: 100%;"></div>
                            </div>
                            <div class="gauge-text ${status === 'UP' ? 'clildrenOn' : 'clildrenOff'}">${status}</div>

                        </div>
                    </div>
                `;

                if (status === 'UP') {
                    upContent += content;
                    countUp++;
                } else {
                    downContent += content;
                    countDown++;
                }
            });
            const output = `
                <span class="txt-status">OFF : <small>${countDown}</small></span>
                <section class="ls">
                    ${downContent}
                </section>
                <span class="txt-status">ON : <small>${countUp}</small></span>
                <section class="ls">
                    ${upContent}
                </section>
            `;
            document.getElementById("main").innerHTML = output;

        });
        buscarMacLocal;
        setInterval(buscarMacLocal, 1000);
    }, function (error) {
        reconnect();
    });
}
socket.onclose = function () {
    console.warn("Conexão fechada. Tentando reconectar...");
    reconnect();
};
function reconnect() {
    setTimeout(() => {
        console.log("Conectando so serviço");
        connectToWebSocket();
    }, 3000)
}
async function Onlw() {

    const statusDiv = document.getElementById("status")

    try {

        const resposta = await fetch(`${endpoint}/endpoint/monitoramento`, {
            method: "GET",
        })
        if (resposta.status === 200) {
            statusDiv.innerHTML = "ON"
            statusDiv.className = "online"
        } else {
            statusDiv.textContent = "Erro de Servidor"
            statusDiv.className = "offline"
        }
    } catch (error) {
        statusDiv.textContent = " DOWN"
        statusDiv.className = "offline"
        window.location.reload()

    }
}

function buscarMac() {
    const select = document.getElementById("stc");
    const client = localStorage.getItem("client");

    const ReceiveGrupoId = select.value;
    if (ReceiveGrupoId != null && ReceiveGrupoId.trim() !== "") {
        localStorage.setItem("ReceiveGrupoId", ReceiveGrupoId);

        stompClient.send(`/app/mestricasM/${client}`, {}, JSON.stringify({
            clientId: client,
            ReceiveGrupoId: ReceiveGrupoId
        }));
    }
}
function buscarMacLocal() {
    let titule = document.getElementById("Monitoramento");

    let loading2 = document.getElementById("overlay-loader");
    let loading = document.getElementById("overlay");
    const client = localStorage.getItem("client");

    const item = localStorage.getItem("ReceiveGrupoId");
    if (item && item.trim() !== "") {
        titule.innerHTML = `Monitoramento de ${item?.replace("-", " ")}`
        stompClient.send("/app/mestricasM", {}, JSON.stringify({
            clientId: client,
            ReceiveGrupoId: item
        }));
    } else {
        titule.style.display = "none"
        loading.style.display = "block";
        loading2.style.display = "none";
        loading.innerHTML = "Selecione um grupo de monitoramento!"
    }

}

function latencia(localizacao, hostid, itemid, ip) {
    let url = `${endpoint}/endpoint/char?hostname=${localizacao}&interfaceIp=${ip}&hostid=${hostid}&itemid=${itemid}&limit=0`;
    window.open(url);
}


document.addEventListener("mouseenter", function (event) {
    if (event.target.matches(".data")) {
        const exibir = event.target.closest(".ms").querySelector(".exibir");
        if (exibir) {
            exibir.style.display = "block";
        }
    }
}, true);
window.addEventListener("DOMContentLoaded", () => {
    itensICMP();
});

async function itensICMP() {
    let section = document.getElementById("stc")
    const response = await fetch(`${endpoint}/itens/group`);
    const itens = await response.json();
    let option = '<option value="">Selecione o grupo</option>';;
    itens.forEach(e => {
        option += `
        <option value="${e.nome}">${e.nome}</option>

    `;
    })
    section.innerHTML = option;
    ;

}
connectToWebSocket();
window.addEventListener("DOMContentLoaded", () => {
    setInterval(() => {
        Onlw()
    }, 1000);
})
