document.addEventListener("DOMContentLoaded", atualizarBotaoAtivo);
document.getElementById("form").addEventListener("submit",function(){
    document.getElementbyId("btnEnviar").disabled = true;
    document.getElementById("btnEnviar").innerHTML = "Enviando";
})
let btn = document.getElementById("btn")
btn.addEventListener("click", () => {
  alert("deu veryo")

})
document.querySelectorAll("[id^='item-']").forEach(function(element) {
element.addEventListener("click",function(){
         let sing = true;
          if(sing){
          alert("deu certo")
          sing=false;
          }else{
          alert("deu errdo")
           sing=true;
          }
       })
});

function app(pagina, bt) {
  let iframe = document.getElementById("iframe");
  iframe.src = pagina;
  document.querySelectorAll(".in button").forEach(button => {
    button.classList.remove("botao");
});

  bt.classList.add("botao")


}
function atualizarBotaoAtivo(){
  document.querySelectorAll(".in button").forEach(btn => {
    if(btn.getAttribute("data-route") === "/lista"){
      btn.classList.add("botao");
    }else{
      btn.classList.remove("botao");
  
    }
 })
 
}








