document.addEventListener("DOMContentLoaded", atualizarBotaoAtivo);

var formEl = document.getElementById("form");
if (formEl) {
    formEl.addEventListener("submit", function () {
        var btnEnviar = document.getElementById("btnEnviar");
        if (btnEnviar) {
            btnEnviar.disabled = true;
            btnEnviar.innerHTML = "Enviando";
        }
    });
}

var btn = document.getElementById("btn");
if (btn) {
    btn.addEventListener("click", function () {
        alert("Deu certo");
    });
}

document.querySelectorAll("[id^='item-']").forEach(function (element) {
    element.addEventListener("click", function () {
        var sing = true;
        if (sing) {
            sing = false;
        } else {
            sing = true;
        }
    });
});

function app(pagina, bt) {
    var iframe = document.getElementById("iframe");
    // Mostrar loading antes de carregar
    var loadingIframe = document.getElementById("iframe-loading");
    if (loadingIframe) loadingIframe.style.display = "flex";
    iframe.src = pagina;
    document.querySelectorAll(".in button").forEach(function (button) {
        button.classList.remove("botao");
    });
    bt.classList.add("botao");
}

function app2(pagina, bt) {
    document.querySelectorAll(".in button").forEach(function (button) {
        button.classList.remove("botao");
    });
    window.location.href = pagina;
    bt.classList.add("botao");
}

function atualizarBotaoAtivo() {
    document.querySelectorAll(".in button").forEach(function (btn) {
        if (btn.getAttribute("data-route") === "/lista") {
            btn.classList.add("botao");
        } else {
            btn.classList.remove("botao");
        }
    });
}
