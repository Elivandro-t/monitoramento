var socket = new SockJS('/ws');  // Ponto de entrada do WebSocket
var stompClient = Stomp.over(socket);
// Função para conectar ao WebSocket
function connectToWebSocket() {
    stompClient.connect({}, function (frame) {
        // Inscrever-se para o destino "/lista"
        stompClient.subscribe('/lista', function (response) {
            let iteml = document.getElementById("item-lenght");

            var data = JSON.parse(response.body);
            // Exibir a lista de endpoints na página
            let output = `
                <table>
                    <thead>
                        <tr>
                            <th><select><option>STATUS</option></select></th>
                            <th><select><option>PROFILE</option></select></th>
                            <th><select><option>HOSTNAME</option></select></th>
                            <th><select><option>MODEL</option></select></th>
                            <th><select><option>S/N</option></select></th>
                            <th><select><option>MAC_ADRESS</option></select></th>
                            <th><select><option>IPv4</option></select></th>
                            <th>CPD</th>
                            <th>HOST GROUP</th>
                        </tr>
                    </thead>
                    <tbody>
            `;
            iteml = data.lenght
            data.forEach(function (item) {

                if(item.status.toUpperCase() === "DOWN"){
                 console.log("Quantidade de itens  off"+data.length)
                    iteml = data.length
                    output += `
                    <tr class="tr" data-name="${item.id}" >
                        <td class="th-text"${item.status.toUpperCase() === 'DOWN' ? 'style="background-color: rgba(255, 0, 0, 0.5)"' : 'style="background-color: rgba(0, 128, 0, 0.8)"'}>${item.status}</td>
                        <td>${item.localizacao}</td>
                        <td>${item.nome}</td>
                        <td>${item.modelo}</td>
                        <td>${item.sn}</td>
                        <td>${item.mac}</td>
                        <td>${item.ip}</td>
                        <td>EM PROCESSO</td>
                        <td>${item.grupo}</td>
                    </tr>
                `;
                }
            });
            console.log("Quantidade de itens "+data.length)
            data.forEach(function (item) {
                if(item.status.toUpperCase() === "UP"){
                  console.log("Quantidade de itens  UP"+data.length)
                    output += `
                    <tr class="tr" data-name="${item.id}" >
                        <td class="th-text"${item.status.toUpperCase() === 'DOWN' ? 'style="background-color: rgba(255, 0, 0, 0.5)"' : 'style="background-color: rgba(0, 128, 0, 0.8)"'}>${item.status}</td>
                        <td>${item.localizacao}</td>
                        <td>${item.nome}</td>
                        <td>${item.modelo}</td>
                        <td>${item.sn}</td>
                        <td>${item.mac}</td>
                        <td>${item.ip}</td>
                        <td>EM PROCESSO</td>
                        <td>${item.grupo}</td>
                    </tr>
                `;
                }
            });
            output += `</tbody></table>`;

            document.getElementById("meuContainer").innerHTML = output;
        });

        // rodarWeb();
    }, function(error) {
        setTimeout(connectToWebSocket, 5000);  // Tentar reconectar após 5 segundos
    });
}
connectToWebSocket();
// stompClient.connect({},function(res){
//     stompClient.subscribe("/lista",function(resposnde){
//         var itens = resposnde.body;
//       console.log(itens)

//       let div ;
//       itens.forEach(function(element) {
//         div = `<div class="itens">
//         <h1>${element.status}</h1>
//         <span>${element.localizacao}</span>
         
//      </div>`;
//       })
//       let container = document.getElementById("meuContainer");

//       container.innerHTML += div;

//     })
// })
function toggleFullscreen() {
    let element = document.getElementById("meuContainer");
        let container = document.getElementById("container_banner");

    if (!document.fullscreenElement) {
        if (element.requestFullscreen) {
            element.requestFullscreen();
        } else if (element.mozRequestFullScreen) { // Firefox
            element.mozRequestFullScreen();
        } else if (element.webkitRequestFullscreen) { // Chrome, Safari e Opera
            element.webkitRequestFullscreen();
        } else if (element.msRequestFullscreen) { // IE/Edge
            element.msRequestFullscreen();
        }
        document.body.style.overflow = 'auto'; 
    } else {
        if (document.exitFullscreen) {
            document.exitFullscreen();
        }
        document.body.style.overflow = ''; // Restaurando o comportamento padrão do overflow

    }
}