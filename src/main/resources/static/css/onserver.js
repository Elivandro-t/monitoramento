
async function Onlw(){
    const statusDiv = document.getElementById("status")
  
    try {
      const resposta  = await fetch("http://10.220.112.112:8083/endpoint/monitoramento",{
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

  async function downloads(){
    try {
      const resposta  = await fetch("http://10.220.112.112:8081/app-downLoads",{
        method:"GET",
      })
      const json = await resposta.json()
      if(json.name!==null){
          window.open(json.url)
      }
    } catch (error) {
      alert("Erro ao baixar app")
    }
  
  }