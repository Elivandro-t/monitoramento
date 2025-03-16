document.addEventListener("DOMContentLoaded", function() {
    let currentMenu = "";

    document.querySelectorAll(".tr").forEach(b => {
        let lista = "";         
        const section = `
        <section class="section">
            <ul class="ul-name">
               <li onclick="deletes('1')">Deletar</li>
               <li onclick=(Atualizar())>Atualizar</li>
               <li onclick=(removeL())>Sair }</li>
            </ul>
        </section>
     `;

        const contextMenuHandler = function(e) {
            e.preventDefault(); 

            let id = e.currentTarget.getAttribute("data-name");
           // Se houver um menu aberto, remova-o
        //    if (currentMenu && currentMenu !== lista && currentMenu.parentNode) {
        //     currentMenu.parentNode.removeChild(currentMenu);
        // }
            if (!lista  ) {
                lista = document.createElement("div");
                lista.setAttribute("class", "td");
                lista.innerHTML = section;
                b.appendChild(lista);
                if(b.includes(lista)){
                    b.removeChild(lista)
                }
                currentMenu = lista;
            }else {

                // Se o menu já estiver visível, remova-o
             
                    b.removeChild(lista);
                    lista = null;
                
            }
        };

        b.addEventListener("contextmenu", contextMenuHandler);

       
    });
    console.log(lista)
    
});

function deletes(isd){
    alert("Em criação "+isd);

}

function removeL(){
  alert("DEU CERTO")

}
function Atualizar(){
    alert("Atualizar")
  
  }
