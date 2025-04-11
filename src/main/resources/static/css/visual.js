function visual(button) {
      let ping = button.getAttribute("data-ping")
      // Find the modal related to this row
      let modal = button.closest('tr').querySelector('.modal');
      modal.style.display = modal.style.display === 'block' ? 'none' : 'block';


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