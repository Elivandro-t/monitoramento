
async function Onlw(){
    const statusDiv = document.getElementById("status")
  
    try {
      const resposta  = await fetch("http://192.168.88.70:8080/endpoint/monitoramento",{
        method:"GET",
      })
      if(resposta.status === 200){
        statusDiv.innerHTML="Serviço ON"
      statusDiv.className="online"
      }else{
        statusDiv.textContent ="Serviço DOWN"
      statusDiv.className= "offline"
      }
    } catch (error) {
      statusDiv.textContent ="Serviço DOWN"
      statusDiv.className="offline"
    }
  }
  window.addEventListener("DOMContentLoaded",()=>{
    setInterval(() => {
    Onlw()
  }, 3000);
  })