var socket = new SockJS('/ws');  // Ponto de entrada do WebSocket
var stompClient = Stomp.over(socket);
let indexs;
stompClient.debug = ()=>{}
function connectToWebSocket() {
    stompClient.connect({}, function (frame) {
        stompClient.subscribe('/lista', function (response) {
            var data = JSON.parse(response.body);
            let downContent = '';
            let upContent = '';
            let countDown = 0;
            let countUp = 0;

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
                <span>OFF : <small>${countDown}</small></span>
                <section class="ls">
                    ${downContent}
                </section>

                <span>ON : <small>${countUp}</small></span>
                <section class="ls">
                    ${upContent}
                </section>
            `;

            document.getElementById("main").innerHTML = output;

        });
    }, function (error) {
        setTimeout(connectToWebSocket, 5000);  // Tentar reconectar após 5 segundos
    });
}

function latencia(localizacao,hostid,itemid,ip) {
    let url = `http://localhost:8080/char?hostname=${localizacao}&interfaceIp=${ip}&hostid=${hostid}&itemid=${itemid}&limit=0`;
    window.location.href = url;
}

document.addEventListener("mouseenter", function (event) {
    if (event.target.matches(".data")) {
        const exibir = event.target.closest(".ms").querySelector(".exibir");
        if (exibir) {
            exibir.style.display = "block";
        }
    }
}, true);
connectToWebSocket();


