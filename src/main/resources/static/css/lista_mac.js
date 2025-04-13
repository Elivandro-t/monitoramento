document.addEventListener("DOMContentLoaded", async function () {
    let currentMenu = null; // menu atualmente aberto
    let dados = /*[[${lista}]]*/[];
    let lista = dados.map(e => e.status)
    document.querySelectorAll(".tr").forEach(async (b) => {

        const contextMenuHandler = async function (e) {
            e.preventDefault();
            let response = await fetch("http://localhost:8080/itens");
            let responseJson = await response.json();
            let nada = "";
            const localizacao = b.getAttribute("data-location")
            const hostid = b.getAttribute("data-id")
            const hostIp = b.getAttribute("data-ip")
            if (responseJson.item && Array.isArray(responseJson.item)) {
                nada = responseJson.item.map(item => `
                                     <li onclick="deletes('${localizacao}','${hostid}','${item.itemid}','${hostIp}')">${item.name}</li>
                                 `).join("");
            } else {
                nada = `<li>Nenhum item encontrado</li>`;
            }
            let section = `
                                             <section class="menud">
                                                 <ul class="ul-name" >
                                                    ${nada}
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

function deletes(localizacao,hostid,itemid,ip) {
    let url = `http://localhost:8080/char?hostname=${localizacao}&interfaceIp=${ip}&hostid=${hostid}&itemid=${itemid}&limit=0`;
    window.location.href = url;
}

function removeL() {
    alert("DEU CERTO")

}
function Atualizar() {
    alert("Atualizar")

}