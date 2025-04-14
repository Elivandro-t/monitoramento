var socket = new SockJS('/ws');  // Ponto de entrada do WebSocket
var stompClient = Stomp.over(socket);
let indexs;
stompClient.debug = ()=>{}
function connectToWebSocket() {
    stompClient.connect({}, function (frame) {
        let loading = document.getElementById("overlay-loader");
        stompClient.subscribe('/lista', function (response) {
            var data = JSON.parse(response.body);
            let downContent = '';
            let upContent = '';
            let countDown = 0;
            let countUp = 0;
            console.log("lista "+data)
            if(data.length < 0){
                loading.style.display = "block";
                return;
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
        buscarMacLocal;
        setInterval(buscarMacLocal(), 20000);  

    }, function (error) {
        reconnect
    });
}
connectToWebSocket();

function reconnect(){
    setTimeout(() => {
      console.log("Conectando so serviço");
      connectToWebSocket();
    }, 5000)
  }
// document.getElementById("form").addEventListener("submit",(e)=>{
// e.preventDefault()
// buscarMac()
// })

function buscarMac() {
    const select = document.getElementById("stc");
    const ReceiveGrupoId = select.value;
    if(ReceiveGrupoId!=null&&ReceiveGrupoId.trim()!=="" ){
        localStorage.setItem("ReceiveGrupoId", ReceiveGrupoId);
        stompClient.send("/app/mestricasM", {}, ReceiveGrupoId);
    }
}
function buscarMacLocal(){
    let loading2 = document.getElementById("overlay-loader");
    let loading = document.getElementById("overlay");
    const item = localStorage.getItem("ReceiveGrupoId");
    if (item && item.trim()!=="") {
        loading.style.display = "none";
        stompClient.send("/app/mestricasM", {}, item);
    }else{
        loading.style.display = "block";
        loading2.style.display = "none";
        loading.innerHTML = "Selecione um grupo de monitoramento!"
    }
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



