let charts = { lat: null, his: null, gau: null };
let stompClient = null;

// Configuração para performance extrema e fim do flicker
Chart.defaults.animation.duration = 0;
Chart.defaults.responsive = true;

function fetchDados() {
  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = null;

  stompClient.connect({}, function (frame) {
    const urlParams = new URLSearchParams(window.location.search);
    const hostId = urlParams.get("hostid");
    const client = localStorage.getItem("client");

    stompClient.subscribe(`/topic/grafico/${client}/${hostId}`, function (response) {
      const data = JSON.parse(response.body);
      if (data.result && data.result.length > 0) {
        const loader = document.getElementById('overlay-loader');
        if (loader) loader.style.display = "none";
        atualizarInterface(data);
      }
    });

    rodaStoP();
    setInterval(rodaStoP, 100); // 1 segundo para tempo real
  }, () => setTimeout(fetchDados, 1000));
}

function rodaStoP() {
  const params = new URLSearchParams(window.location.search);
  if (stompClient?.connected) {
    stompClient.send("/app/metricas", {}, JSON.stringify({
      hostid: params.get("hostid"),
      clientId: localStorage.getItem("client")
    }));
  }
}

function atualizarInterface(data) {
  const urlParams = new URLSearchParams(window.location.search);
  const dados = data.result;

  // --- LOGICA PARA EXIBIR O IP NO BADGE ---
  const ipElement = document.getElementById('ip-badge');
  if (ipElement) {
    const ipDaUrl = urlParams.get("interfaceIp");
    const ipDoJson = dados[0]?.hostIp || dados[0]?.ip || dados[0]?.interfaceIp;
    ipElement.innerText = `IP: ${ipDaUrl || ipDoJson || "0.0.0.0"}`;
  }

  const titleElement = document.getElementById('titule');
  if (titleElement) {
    titleElement.innerText = urlParams.get("hostname") || "Equipamento";
  }

  // Ordenação e Filtragem
  const latSet = dados.filter(e => e.icmpName === "ICMP response time").sort((a,b) => a.clock - b.clock);
  const lossSet = dados.filter(e => e.icmpName === "ICMP loss").sort((a,b) => a.clock - b.clock);

  const formatarHora = (timestamp) => {
    return new Date(timestamp * 1000).toLocaleTimeString("pt-BR", {
      timeZone: 'America/Sao_Paulo', // Resolve o atraso de 1 hora
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
    });
  };

  // PERFORMANCE: Mostra 15 pontos para o gráfico correr fluido
  const maxPontos = 15;

  if (latSet.length > 0) {
    const slice = latSet.slice(-maxPontos);
    const labels = slice.map(e => formatarHora(e.clock));
    const values = slice.map(v => (v.value * 1000).toFixed(2));
    renderQueFlui('lat', 'latencyChart', values, labels, '#58a6ff', 'rgba(88, 166, 255, 0.1)', false);
  }

  if (lossSet.length > 0) {
    const slice = lossSet.slice(-maxPontos);
    const labels = slice.map(e => formatarHora(e.clock));

    // CORREÇÃO: Arredonda para inteiro para ficar "0%" e não "0.00%"
    const values = slice.map(e => Math.round(Number(e.value)));

    renderQueFlui('his', 'icmpChart', values, labels, '#f85149', 'transparent', true);
    renderGaugeChart(values[values.length - 1]);
  }
}

function renderQueFlui(key, canvasId, newData, newLabels, color, fill, isPercentage) {
  const ctx = document.getElementById(canvasId).getContext('2d');

  if (charts[key]) {
    charts[key].data.labels = newLabels;
    charts[key].data.datasets[0].data = newData;
    charts[key].update('none'); // Update seco para não ter delay
  } else {
    charts[key] = new Chart(ctx, {
      type: 'line',
      data: {
        labels: newLabels,
        datasets: [{
          data: newData,
          borderColor: color,
          backgroundColor: fill,
          fill: fill !== 'transparent',
          tension: 0.3, pointRadius: 0, borderWidth: 2
        }]
      },
      options: {
        responsive: true, maintainAspectRatio: false, animation: false,
        scales: {
          x: { display: true, grid: { display: false } },
          y: {
            beginAtZero: true,
            min: isPercentage ? 0 : undefined,
            max: isPercentage ? 100 : undefined,
            grid: { color: '#30363d' }
          }
        },
        plugins: { legend: { display: false } }
      }
    });
  }
}

function renderGaugeChart(percent) {
  const ctx = document.getElementById('gaugeChart').getContext('2d');

  // CORREÇÃO: Apenas números inteiros
  const p = Math.round(percent);
  const color = p > 10 ? '#f85149' : '#3fb950';

  if (charts.gau) {
    charts.gau.data.datasets[0].data = [p, 100 - p];
    charts.gau.data.datasets[0].backgroundColor = [color, '#30363d'];
    charts.gau.update('none');
  } else {
    charts.gau = new Chart(ctx, {
      type: 'doughnut',
      data: {
        datasets: [{
          data: [p, 100 - p],
          backgroundColor: [color, '#30363d'],
          borderWidth: 0
        }]
      },
      options: {
        rotation: -90,
        circumference: 180,
        cutout: '80%',
        animation: false,
        events: [] // Otimiza performance
      }
    });
  }
  document.getElementById('percentLabel').innerText = `${p}%`;
  document.getElementById('percentLabel').style.color = color;
}

window.addEventListener('load', fetchDados);