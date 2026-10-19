let base = "http://10.220.122.96:8083";

document.addEventListener("DOMContentLoaded", function () {
    let currentMenu = null;

    // 1. Lógica do Menu de Contexto (Botão Direito)
    document.querySelectorAll(".tr").forEach((b) => {
        b.addEventListener("contextmenu", function (e) {
            e.preventDefault();
            const localizacao = b.getAttribute("data-location");
            const hostid = b.getAttribute("data-id");
            const hostIp = b.getAttribute("data-ip");

            if (currentMenu) currentMenu.remove();

            const menu = document.createElement("div");
            menu.classList.add("context-container");
            menu.innerHTML = `
                <section class="menud">
                    <ul class="ul-name">
                        <li onclick="metricas('${localizacao}','${hostid}','${hostIp}')">Métricas</li>
                        <li onclick="Atualizar('${hostid}')">Editar</li>
                        <li onclick="deletar('${hostid}')">Deletar</li>
                    </ul>
                </section>`;

            menu.style.position = "absolute";
            menu.style.zIndex = "10000";
            document.body.appendChild(menu);

            // Ajuste para o menu não sair da tela
            let posX = e.pageX;
            let posY = e.pageY;
            const menuWidth = menu.offsetWidth;
            const menuHeight = menu.offsetHeight;

            if (posX + menuWidth > window.innerWidth + window.scrollX) posX -= menuWidth;
            if (posY + menuHeight > window.innerHeight + window.scrollY) posY -= menuHeight;

            menu.style.top = `${posY}px`;
            menu.style.left = `${posX}px`;
            currentMenu = menu;
        });
    });

    // Fecha o menu ao clicar fora
    document.addEventListener("click", (e) => {
        if (currentMenu && !currentMenu.contains(e.target)) {
            currentMenu.remove();
            currentMenu = null;
        }
    });

    // 2. Lógica de Ordenação da Tabela
    document.querySelectorAll("th").forEach((headerCell, index) => {
        headerCell.style.cursor = "pointer";
        headerCell.addEventListener("click", () => {
            const tableElement = headerCell.closest("table");
            const tbody = tableElement.querySelector("tbody");
            const currentIsAscending = headerCell.classList.contains("th-sort-asc");

            sortDataTable(tbody, index, !currentIsAscending);

            tableElement.querySelectorAll("th").forEach(th => th.classList.remove("th-sort-asc", "th-sort-desc"));
            headerCell.classList.toggle("th-sort-asc", !currentIsAscending);
            headerCell.classList.toggle("th-sort-desc", currentIsAscending);
        });
    });
});

/**
 * Ordenação otimizada para Texto, Números e IPs
 */
function sortDataTable(tbody, columnIndex, asc = true) {
    const dirModifier = asc ? 1 : -1;
    const rows = Array.from(tbody.querySelectorAll("tr.tr"));

    const sortedRows = rows.sort((a, b) => {
        const aText = a.children[columnIndex].textContent.trim();
        const bText = b.children[columnIndex].textContent.trim();

        // Usa localeCompare com numeric:true para ordenar IPs e números corretamente (Ex: 2 < 10)
        return aText.localeCompare(bText, undefined, { numeric: true, sensitivity: 'base' }) * dirModifier;
    });

    // AppendChild em um loop remove o elemento da posição antiga e coloca na nova automaticamente
    sortedRows.forEach(row => tbody.appendChild(row));
}

// 3. Funções Globais de Ação
function metricas(localizacao, hostid, ip) {
    window.location.href = `${base}/endpoint/monitoramento/char?hostname=${localizacao}&interfaceIp=${ip}&hostid=${hostid}&limit=0&itemid=0`;
}

function Atualizar(id) {
    window.location.href = `${base}/endpoint/registro?id=${id}`;
}

async function verificarToken(token) {
    if (!token) return false;
    try {
        const response = await fetch('/auth/verificar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ token: token })
        });
        const data = await response.json();
        return data.autorizado === true;
    } catch (err) {
        console.error('Erro ao verificar token:', err);
        return false;
    }
}

async function realizarLogin() {
    const username = prompt('Usuario:', 'cupsadmin');
    if (!username) return null;
    const senha = prompt('Senha:');
    if (!senha) return null;

    try {
        const response = await fetch('/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: username, senha: senha })
        });
        const data = await response.json();
        if (data.token) {
            localStorage.setItem('auth_token', data.token);
            return data.token;
        }
        alert('Usuario ou senha invalidos');
        return null;
    } catch (err) {
        console.error('Erro ao realizar login:', err);
        alert('Erro de rede ao autenticar');
        return null;
    }
}

const deletar = async (id) => {
    let token = localStorage.getItem('auth_token');
    if (!(await verificarToken(token))) {
        token = await realizarLogin();
        if (!token) return;
    }

    if (!confirm('Deseja realmente deletar este registro?')) return;

    try {
        const response = await fetch(`/endpoint/delete?id=${id}`, {
            method: 'DELETE',
            headers: { 'Authorization': token }
        });
        if (response.ok) {
            alert('Registro deletado!');
            location.reload();
        } else {
            localStorage.removeItem('auth_token');
            alert('Sessao expirada ou sem permissao. Faça login novamente.');
        }
    } catch (err) {
        console.error('Erro na requisição:', err);
        alert('Erro ao deletar');
    }
};