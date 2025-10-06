let chartInstance = null;
let chartInstance2 = null;
let lenght = 0;
let chartInstance3= null;
const endpoint = "http://192.168.88.70:8080";
async function itensICMP() {
  const response = await fetch(`${endpoint}/itens`);
  const itens = await response.json();
  return {itens};
}
var socket = new SockJS('/ws');
var stompClient = Stomp.over(socket);
stompClient.debug = () => {};
 function fetchDados() {
  const client = localStorage.getItem("client");
  const url = new URLSearchParams(window.location.search);
  const ipv4 = url.get("interfaceIp");
  const hostname = url.get("hostname");
  let loading = document.getElementById('overlay-loader')
    stompClient.connect({}, function (frame) {
      stompClient.subscribe(`/topic/grafico/${client}`, function (response) {
        var data = JSON.parse(response.body);
        if(data.result.length<0){
          loading.style.display = "block"
        }else{
          atualizarGrafico(data,hostname,ipv4);
          loading.style.display = "none"
        }
      })
      // rodaStoP()
      setInterval(rodaStoP, 5000);  
    }, function (error) {
      reconnect();
    })
  
  };
  socket.onclose = function () {
    console.warn("Conexão fechada. Tentando reconectar...");
    reconnect();
  };

  function reconnect(){
    setTimeout(() => {
      console.log("Conectando ao serviço");
      fetchDados();
    }, 1000)
  }
  function rodaStoP(){
    const client = localStorage.getItem("client");

    const url = new URLSearchParams(window.location.search);
        const host = url.get("hostid")
        stompClient.send("/app/metricas", {}, JSON.stringify({
          hostid: host,
          clientId:client
        }));
  }
  async  function atualizarGrafico(dataNames,hostname,ipv4) {
  const dadosName = dataNames;
  let titule = document.getElementById('titule')
  let dados = dadosName?.result;
  const {itens} = await itensICMP();
  const itemICMP0 = dados[0].itemId;
  const itemICMP1 = dados[1].itemId;
  const dadosPerda = dados.filter(e => e.icmpName === "ICMP loss");
  const dadosLatencia = dados.filter(e => e.icmpName === "ICMP response time");
  titule.innerHTML = hostname;
  const labelsclock = dadosLatencia.map(e => {
    const ts = new Date(e.clock * 1000).toLocaleString("pt-BR");
    return ts;
  });
  const labelsclocke = dadosPerda.map(e => {
    const ts = new Date(e.clock * 1000).toLocaleString("pt-BR");
    return ts;
  });

itens.item.map(element => {
  if(element.itemid==itemICMP1){
    graficoICMPPerdas(dadosPerda.map(e => Number(e.value)),ipv4,dados);

  }if(element.itemid==itemICMP0){
    graficoICMPSec(dadosLatencia,ipv4,labelsclock,element.name);

  }
});


function graficoICMPPerdas(datas,ipv4,dados){
  const data = {
    labels: labelsclocke.reverse(),
    datasets: [{
      label: "Perdas" ,
      data: datas.reverse(),
      fill: false,
      borderColor: 'rgba(35, 192, 192, 0.3)',
      backgroundColor: 'rgba(81, 207, 207, 0.2)',
      borderWidth:2,
      pointRadius: 2
    }]
  };

  const config = {
    type: 'line',
    data: data,
    options: {
      responsive: true,
      layout: {
        padding: {
          right: 1
        }
      }, 
      scales: {
        y: {
          beginAtZero: true,
          min:0,
          max:100,
          title: {
            display: true,
            text: 'Perda'
          }
        },
        x: {
          title: {
            display: true,
            text: 'Amostra'
          },
          display: true
        }
      },
      plugins: {
        tooltip: {
          callbacks: {
            label: function (context) {
              return context.parsed.y + '%' + " IPV4 "+ipv4;
            }
          }
        }
      }
    }
  };
  if (chartInstance3) {
    chartInstance3.destroy()
  }
  chartInstance3 = new Chart(document.getElementById('icmpChart'), config);
  const percent = dados[1].value;
  const ctxf = document.getElementById('gaugeChart').getContext('2d');
  const percentLabel = document.getElementById('percentLabel');
  if (chartInstance2) {
    chartInstance2.destroy()
  }

  chartInstance2 = new Chart(ctxf, {
    type: 'doughnut',
    data: {
      datasets: [{
        data: [percent, 100 - percent],
        backgroundColor: percent < 50
          ? ['#81c784', '#2e2e2e'] // verde se for menor que 50
          : ['#e57373', '#2e2e2e'],
        borderWidth: 0
      }]
    },
    options: {
      rotation: -90,
      circumference: 180,
      cutout: '80%',
      responsive: true,
      plugins: {
        tooltip: { enabled: false },
        legend: { display: false }
      }
    }
  });
  if (percent > 50) {
    percentLabel.style.color = "#f12011";
  } else {
    percentLabel.style.color = "#61c061";
  }

  percentLabel.textContent = `${percent}%`;
 }
}

function graficoICMPSec(dados,ipv4,labelsclock,labelss){

  const valuesInMs = dados.map(v => parseFloat((v.value*1000).toFixed(2)));

  const ctx = document.getElementById('latencyChart').getContext('2d');
  if (chartInstance) {
    chartInstance.destroy()
  }
  chartInstance = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: labelsclock.reverse(),
      datasets: [{
        label: labelss,
        data: valuesInMs.reverse(),
        borderColor: 'green',
        backgroundColor: 'rgba(0, 255, 0, 0.3)', // preenchimento verde claro
        fill: true,
        tension: 0.3,
        pointRadius: 1,
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      layout: {
        padding: {
          right:1
        }
      }, 
      scales: {
        y: {
          max: 100000,
          beginAtZero: true,
          ticks: {
            color: 'white',
            callback: (value) => value + ' ms'
          }
        },
        x: {
          display: false
        }
      },

      plugins: {
        legend: {
          labels: {
            color: 'white'
          }
        },
        tooltip: {
          callbacks: {
            label: function (context) {
              return context.parsed.y + 'ms' + " " + ipv4;
            }
          }
        },
        title: {
          display: true,
          text: 'ICMP Response Time',
          color: 'white'
        }
      }
    }
  });

}

fetchDados();
