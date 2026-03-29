let cliente = null;

function visual(button) {
    console.log("1. Clique detectado no botão");

    const tr = button.closest('tr');
    if (!tr) {
        console.error("Erro: Não encontrei a linha (TR)");
        return;
    }

    const modal = tr.querySelector('.modal');
    if (!modal) {
        console.error("Erro: Não encontrei a classe .modal");
        return;
    }

    console.log("2. Exibindo modal centralizado...");

    // Configurações para garantir que o modal sobreponha tudo e centralize
    Object.assign(modal.style, {
        display: "block",
        position: "fixed",
        top: "50%",
        left: "50%",
        transform: "translate(-50%, -50%)",
        zIndex: "10000", // Valor alto para ficar na frente de tudo
        backgroundColor: "#1a1a1a", // Cor de fundo para não ficar transparente
        padding: "10px",
        borderRadius: "5px",
        boxShadow: "0px 0px 20px rgba(0,0,0,0.5)"
    });

    const ip = tr.getAttribute("data-ip");
    ativaCMD(ip, modal);
}

function iniciarConexao(ip, modalAtual) {
    const client = localStorage.getItem("client");
    const socket = new SockJS("/ws");
    cliente = Stomp.over(socket);

    // Opcional: Desativar logs do console para limpar o terminal
    cliente.debug = null;

    cliente.connect({}, function (frame) {
        console.log("Socket Conectado");
        let logContainer = modalAtual.querySelector("#container12") || modalAtual.querySelector(".area-propt");

        if (logContainer) {
            logContainer.innerHTML = `<div style="color: #00ff00">> Conectado a ${ip}. Aguardando...</div>`;
        }

        cliente.subscribe(`/topic/cmd/${client}`, function (response) {
            if (logContainer) {
                logContainer.innerHTML += `<div>${response.body}</div>`;
                logContainer.scrollTop = logContainer.scrollHeight;
            }
        });

        cliente.send("/app/cmd", {}, JSON.stringify({ ip: ip, hostID: client }));
    }, function(err) {
        console.error("Erro de conexão:", err);
        let logContainer = modalAtual.querySelector("#container12") || modalAtual.querySelector(".area-propt");
        if (logContainer) {
            logContainer.innerHTML = `<div style="color: #ff4444">> Dispositivo ${ip} offline ou erro de rede.</div>`;
        }
        cliente = null;
    });
}

function ativaCMD(ip, modal) {
    if (cliente && cliente.connected) {
        cliente.disconnect(() => {
            cliente = null;
            iniciarConexao(ip, modal);
        });
    } else {
        iniciarConexao(ip, modal);
    }
}

// ESTA FUNÇÃO É A QUE FECHA O MODAL
function fecharModal(button) {
    // 1. Localiza o modal subindo a árvore do DOM a partir do botão clicado
    const modal = button.closest('.modal');

    if (modal) {
        modal.style.display = "none";
        console.log("Modal fechado visualmente.");
    }

    // 2. Desconecta o socket se existir, mas sem bloquear o fechamento
    if (cliente) {
        try {
            cliente.disconnect();
        } catch (e) {
            console.warn("Erro ao desconectar:", e);
        } finally {
            cliente = null;
        }
    }
}