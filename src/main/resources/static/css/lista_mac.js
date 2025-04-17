let base = "http://localhost:8080"
document.addEventListener("DOMContentLoaded", async function () {
    let currentMenu = null; // menu atualmente aberto
    let dados = /*[[${lista}]]*/[];
    let lista = dados.map(e => e.status)
    document.querySelectorAll(".tr").forEach(async (b) => {
        const contextMenuHandler = async function (e) {
            e.preventDefault();
            const localizacao = b.getAttribute("data-location")
            const hostid = b.getAttribute("data-id")
            const hostIp = b.getAttribute("data-ip");
    
            let section = `
                          <section class="menud">
                                <ul class="ul-name" >
                                    <li onclick="metricas('${localizacao}','${hostid}','${hostIp}')">Metricas</li>
                                    <li onclick="Atualizar('${hostid}')">Editar</li>
                                    <li onclick="Deletar('${hostid}')">Excluir</li>
                                </ul>
                          </section>
                                         `;
            // Remove menu atual, se existir
            if (currentMenu) {
                currentMenu.remove();
                currentMenu = null;
            }

            // Cria novo menu
            const menu = document.createElement("div");
            menu.classList.add("td");
            menu.innerHTML = section;
            menu.style.position = "absolute";
    menu.style.top = `${e.pageY}px`;
    menu.style.left = `${e.pageX}px`;
    menu.style.zIndex = 9999;
            // Adiciona o menu à TR clicada
            b.appendChild(menu);
            currentMenu = menu;
        };

        // Adiciona o evento de clique direito na TR
        b.addEventListener("contextmenu", contextMenuHandler);

});
    // Fecha o menu ao clicar fora
    document.addEventListener("click", (e) => {
        if (currentMenu && !currentMenu.contains(e.target)) {
            currentMenu.remove();
            currentMenu = null;
        }
    });

});

function metricas(localizacao,hostid,ip) {
    let url = `${base}/char?hostname=${localizacao}&interfaceIp=${ip}&hostid=${hostid}&limit=0&itemid=0`;
    window.location.href = url;
}

function Deletar(id) {
    alert("EM desenvolvimento "+id)

}
function Atualizar(id) {
   window.location.href = `${base}/registro?id=${id}`

}