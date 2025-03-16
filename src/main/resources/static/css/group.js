
document.addEventListener("DOMContentLoaded", function () {
  let modal = document.getElementById("modal");
  if (modal && modal.textContent.trim() !== "") {
    setTimeout(() => {
      modal.style.display = "none"; // Oculta o modal após 3 segundos
    }, 5000);
  }
});
document.addEventListener("DOMContentLoaded", function () {
const checkbox = document.getElementById("checkbox")
if(checkbox){
  checkbox.addEventListener("change",function(event){
    let arquivo  = document.getElementById("arquivo")
    let form  = document.getElementById("form")
    if(event.target.checked){
     arquivo.style.display = "block"
     form.style.display = "none"
     console.log("ckeck")
    }else{
     arquivo.style.display = "none"
     form.style.display = "block"
    }
  })
}
})