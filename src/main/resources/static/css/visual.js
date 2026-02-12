let cliente = null;
let ipContent = null
const lista = []
function iniciarConexao(ip) {
    const client = localStorage.getItem("client");

    const socket = new SockJS("/ws");
    cliente = Stomp.over(socket);
    ipContent = ip
    cliente.connect({}, function () {
        cliente.subscribe(`/topic/cmd/${client}`, function (response) {
            ipContent = null;
            lista.push(response.body)   
            let s1 =  document.getElementById("container12");
            s1.innerHTML += "elivanro"      

        });

        if (ipContent != null) {
            cliente.send("/app/cmd", {}, JSON.stringify({ ip: ipContent, hostID: client }));
        }
    });
}
function fecharModal(button) {
    const modal = button.closest('tr').querySelector('.modal');
    modal.style.display = "none";

    if (cliente && cliente.connected) {
        cliente.disconnect(() => {
            cliente = null
            ipContent = null
            window.location.reload()
        });
    }
}
function ativaCMD(ip) {
    if (cliente && cliente.connected) {
        cliente.disconnect(() => {
            cliente = null
            ipContent = null
        });
    } else {
        iniciarConexao(ip);
    }
}


function visual(button) {
    let modal = button.closest('tr').querySelector('.modal');
    modal.style.display = modal.style.display === 'block' ? 'none' : 'block';
    const ipElement = modal.querySelector(".area-propt");
    const ip = ipElement.textContent.trim();
   


    ativaCMD(ip);

}

document.addEventListener("DOMContentLoaded", function (e) {
    e.preventDefault();
    let movin = false; // Flag para controlar o movimento
    let offsetX, offsetY; // Variáveis para armazenar o deslocamento do mouse
    // Função para o movimento do modal
    function getmove(e, modal) {
        if (movin) {
            modal.style.left = (e.clientX - offsetX) + "px";
            modal.style.top = (e.clientY - offsetY) + "px";
        }
    }
    // Atribuindo evento de arrastar a cada modal
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        modal.onmousedown = function (event) {
            event.preventDefault();
            offsetX = event.clientX - modal.getBoundingClientRect().left;
            offsetY = event.clientY - modal.getBoundingClientRect().top;
            movin = true;
            // Adiciona o evento de movimento do mouse
            document.addEventListener("mousemove", (e) => getmove(e, modal));
            // Para o movimento quando o mouse for solto
            modal.onmouseup = function () {
                movin = false;
                document.removeEventListener("mousemove", (e) => getmove(e, modal));
                modal.onmouseup = null;
            }
        };
    });

    // Reinicializa o movimento quando a janela ganhar ou perder o foco
    window.addEventListener('focus', function () {
        movin = false;
    });
    window.addEventListener('blur', function () {
        movin = false;
    });
});

function toggleFullscreen() {
    let element = document.getElementById("meuContainer");
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