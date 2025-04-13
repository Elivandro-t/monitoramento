let chartInstance = null;
    let chartInstance2 = null;
    async function fetchDados() {
      const url = new URLSearchParams(window.location.search);
      const item = url.get("itemid");
      const host = url.get("hostid")
      const response = await fetch('http://localhost:8080/mestricasHost', {
        method: "POST",
        headers: { 'Content-type': 'application/json' },
        body: JSON.stringify({
          hostid: host
        })

      });
      const dadosName = await response.json();
      return { dadosName};
    }

    async function atualizarGrafico() {
      const { dadosName} = await fetchDados();

      let titule = document.getElementById('titule')
      let latencyChart = document.getElementById('icmpChart')
      let latenciap = document.getElementById('icmpChart')
      let latenc = document.getElementById('latencyChart')
      let canv = document.getElementById('canv')
      let dados = dadosName.result;
      const labelss = dados[0].icmpName;
      const item = dados[0].itemId;
      const ultimaData = new Date(dados[0].clock * 1000).toLocaleString("pt-BR");
      titule.innerHTML = labelss;// Usar o índice como label
      const datas = dados.map(e => Number(e.value));

      console.log("todas as minhas datas " + ultimaData)
      const labelsclock = dados.map(e => {
        const ts = new Date(e.clock * 1000).toLocaleString("pt-BR");
        return ts;
      });

      if (item === 1) {
        latenc.style.display = "none"
        const data = {
          labels: labelsclock.reverse(),
          datasets: [{
            label: labelss,
            data: datas.reverse(),
            fill: false,
            borderColor: 'rgba(75, 192, 192, 1)',
            backgroundColor: 'rgba(75, 192, 192, 0.2)',
            tension: 0.3,
            pointRadius: 2
          }]
        };

        const config = {
          type: 'line',
          data: data,
          options: {
            responsive: true,
            scales: {
              y: {
                beginAtZero: true,
                title: {
                  display: true,
                  text: 'Perda'
                }
              },
              x: {
                // title: {
                //   display: true,
                //   text: 'Amostra'
                // }
                display: false
              }
            },
            plugins: {
              tooltip: {
                callbacks: {
                  label: function (context) {
                    return context.parsed.y + '%';
                  }
                }
              }
            }
          }
        };
        if (chartInstance) {
          chartInstance.destroy()
        }
        chartInstance = new Chart(document.getElementById('icmpChart'), config);
        const percent = dados[0].value;
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
              backgroundColor:  percent < 50 
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
      else {

        const valuesInMs = dados.map(v => parseFloat((v.value * 1000).toFixed(2)));
        latencyChart.style.display = "none"
        canv.style.display = "none";
        const ctx = document.getElementById('latencyChart').getContext('2d');
        latenciap.style.display = "none"
        if (chartInstance) {
          chartInstance.destroy()
        }
        chartInstance = new Chart(ctx, {
          type: 'bar',
          data: {
            labels: labelsclock.reverse(),
            datasets: [{
              label: labelss.replace(", ","as"),
              data: valuesInMs.reverse(),
              borderColor: 'green',
              backgroundColor: 'rgba(0, 255, 0, 0.3)', // preenchimento verde claro
              fill: true,
              tension: 0.3,
              pointRadius: 0,
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false, // Deixa o tamanho livre
            scales: {
              y: {
                max: 2000,
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
                    return context.parsed.y + 'ms';
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
    }

    setInterval(atualizarGrafico, 30000);

    atualizarGrafico();