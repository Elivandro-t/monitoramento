var charts = { lat: null, his: null, gau: null, jit: null, rng: null };
var stompClient = null;
// Padrao: 1 minuto
var horasFiltro = 0;
var secondsFiltro = 60;
var intervaloEnvio = null;
var ultimoClockRecebido = 0;
var ultimoTamanhoRecebido = 0;
var ignorarProximaComparacao = false;

Chart.defaults.animation.duration = 0;
Chart.defaults.responsive = true;

function fetchDados() {
    var urlParams = new URLSearchParams(window.location.search);

    // Sempre inicia com 1m no carregamento da pagina (usuario pode trocar apos)
    horasFiltro = 0;
    secondsFiltro = 60;
    localStorage.setItem('chart_filter', '0:60');

    adicionarFiltros();

    // Save selected hostid to localStorage for persistence on reload
    var hostId = urlParams.get("hostid");
    if (hostId) {
        localStorage.setItem('chart_hostid', hostId);
    }
    var hostname = urlParams.get("hostname");
    if (hostname) {
        localStorage.setItem('chart_hostname', hostname);
    }
    var interfaceIp = urlParams.get("interfaceIp");
    if (interfaceIp) {
        localStorage.setItem('chart_interfaceIp', interfaceIp);
    }

    // If no hostid in URL, try localStorage
    if (!hostId) {
        hostId = localStorage.getItem('chart_hostid');
        if (hostId) {
            var newUrl = window.location.pathname + '?hostid=' + hostId;
            var savedHostname = localStorage.getItem('chart_hostname');
            var savedIp = localStorage.getItem('chart_interfaceIp');
            if (savedHostname) newUrl += '&hostname=' + encodeURIComponent(savedHostname);
            if (savedIp) newUrl += '&interfaceIp=' + encodeURIComponent(savedIp);
            window.history.replaceState(null, '', newUrl);
        }
    }

    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        var urlParams2 = new URLSearchParams(window.location.search);
        var hostId2 = urlParams2.get("hostid");
        var client = localStorage.getItem("client");

        stompClient.subscribe('/topic/grafico/' + client + '/' + hostId2, function (response) {
            var data = JSON.parse(response.body);
            if (data.result && Object.keys(data.result).length > 0) {
                var loader = document.getElementById('overlay-loader');
                if (loader) loader.style.display = "none";

                // Evita re-render quando nada mudou (remove o "piscar")
                var arr = data.result;
                var maxClock = 0;
                for (var k = 0; k < arr.length; k++) {
                    var c = Number(arr[k].clock || 0);
                    if (c > maxClock) maxClock = c;
                }
                if (!ignorarProximaComparacao
                        && maxClock === ultimoClockRecebido
                        && arr.length === ultimoTamanhoRecebido) {
                    return; // dados identicos, nao atualiza nada
                }
                ultimoClockRecebido = maxClock;
                ultimoTamanhoRecebido = arr.length;
                ignorarProximaComparacao = false;

                atualizarInterface(data);
            }
        });

        enviarMetricas();
        reagendarPolling();
    }, function() {
        setTimeout(fetchDados, 1000);
    });
}

function reagendarPolling() {
    if (intervaloEnvio) clearInterval(intervaloEnvio);
    // Ajusta frequencia de polling conforme a janela de tempo selecionada
    var totalSeg = (horasFiltro * 3600) + secondsFiltro;
    var intervalMs;
    if (totalSeg === 0) intervalMs = 10000;         // Todos: 10s
    else if (totalSeg <= 60) intervalMs = 1000;     // <= 1m: 1s
    else if (totalSeg <= 300) intervalMs = 2000;    // <= 5m: 2s
    else if (totalSeg <= 3600) intervalMs = 5000;   // <= 1h: 5s
    else if (totalSeg <= 21600) intervalMs = 15000; // <= 6h: 15s
    else intervalMs = 30000;                        // > 6h: 30s
    intervaloEnvio = setInterval(enviarMetricas, intervalMs);
}

function enviarMetricas() {
    var params = new URLSearchParams(window.location.search);
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/metricas", {}, JSON.stringify({
            hostid: params.get("hostid"),
            clientId: localStorage.getItem("client"),
            hours: horasFiltro,
            seconds: secondsFiltro
        }));
    }
}

function adicionarFiltros() {
    var filter = document.getElementById('horas-filtro');
    if (!filter) return;

    // Set the current filter value
    var filterValue = horasFiltro + ':' + secondsFiltro;
    for (var i = 0; i < filter.options.length; i++) {
        if (filter.options[i].value === filterValue) {
            filter.selectedIndex = i;
            break;
        }
    }

    filter.addEventListener('change', function () {
        var parts = this.value.split(':');
        horasFiltro = Number(parts[0]) || 0;
        secondsFiltro = Number(parts[1]) || 0;
        localStorage.setItem('chart_filter', this.value);
        document.getElementById('overlay-loader').style.display = 'flex';
        // forca render na proxima resposta (dados diferentes agora)
        ignorarProximaComparacao = true;
        ultimoClockRecebido = 0;
        ultimoTamanhoRecebido = 0;
        enviarMetricas();
        reagendarPolling();
    });
}

function atualizarInterface(data) {
    var urlParams = new URLSearchParams(window.location.search);
    var dados = data.result;

    // Header
    var ipElement = document.getElementById('ip-badge');
    if (ipElement) {
        var ipDaUrl = urlParams.get("interfaceIp") || localStorage.getItem('chart_interfaceIp');
        ipElement.innerText = 'IP: ' + (ipDaUrl || "0.0.0.0");
    }

    var titleElement = document.getElementById('titule');
    if (titleElement) {
        titleElement.innerText = urlParams.get("hostname") || localStorage.getItem('chart_hostname') || "Equipamento";
    }

    var totalFiltroSegundos = (horasFiltro * 3600) + secondsFiltro;
    var maxPontos;
    if (totalFiltroSegundos > 0 && totalFiltroSegundos <= 60) {
        maxPontos = Math.min(60, dados.length);
    } else if (totalFiltroSegundos <= 300) {
        maxPontos = Math.min(120, dados.length);
    } else {
        maxPontos = horasFiltro > 0 ? Math.min(260, Math.max(40, horasFiltro * 10)) : 260;
    }

    // Filtra por tipo de metrica
    var latSet = dados.filter(function(e) { return e.icmpName === "ICMP response time"; }).sort(function(a,b) { return a.clock - b.clock; });
    var lossSet = dados.filter(function(e) { return e.icmpName === "ICMP loss"; }).sort(function(a,b) { return a.clock - b.clock; });
    var jitterSet = dados.filter(function(e) { return e.icmpName === "ICMP jitter"; }).sort(function(a,b) { return a.clock - b.clock; });
    var minSet = dados.filter(function(e) { return e.icmpName === "ICMP min latency"; }).sort(function(a,b) { return a.clock - b.clock; });
    var maxSet = dados.filter(function(e) { return e.icmpName === "ICMP max latency"; }).sort(function(a,b) { return a.clock - b.clock; });

    // Status badge
    var statusBadge = document.getElementById('status-badge');
    if (statusBadge && latSet.length > 0) {
        var ultimaLatencia = Number(latSet[latSet.length - 1].value);
        if (ultimaLatencia > 0) {
            statusBadge.innerText = 'ONLINE';
            statusBadge.className = 'status-badge status-online';
        } else {
            statusBadge.innerText = 'OFFLINE';
            statusBadge.className = 'status-badge status-offline';
        }
    }

    // Offline summary
    var offlineSummary = document.getElementById('offline-summary');
    if (offlineSummary) {
        var offlinePoints = lossSet.filter(function(e) { return Number(e.value) >= 100; }).length;
        var totalPoints = Math.max(lossSet.length, latSet.length, 1);
        offlineSummary.innerText = 'Offline aprox.: ' + offlinePoints + ' / ' + totalPoints + ' pontos';
    }

    // ========== STATS CARDS ==========
    if (latSet.length > 0) {
        var ultimoAvg = Number(latSet[latSet.length - 1].value);
        document.getElementById('stat-avg').innerText = ultimoAvg.toFixed(2) + ' ms';
        var avgEl = document.getElementById('stat-avg');
        avgEl.className = 'stat-value' + (ultimoAvg > 150 ? ' stat-bad' : ultimoAvg > 50 ? ' stat-warn' : ' stat-good');
    }
    if (minSet.length > 0) {
        document.getElementById('stat-min').innerText = Number(minSet[minSet.length - 1].value).toFixed(2) + ' ms';
    }
    if (maxSet.length > 0) {
        var maxVal = Number(maxSet[maxSet.length - 1].value);
        document.getElementById('stat-max').innerText = maxVal.toFixed(2) + ' ms';
        var maxEl = document.getElementById('stat-max');
        maxEl.className = 'stat-value' + (maxVal > 200 ? ' stat-bad' : ' stat-warn');
    }
    if (jitterSet.length > 0) {
        var jitterVal = Number(jitterSet[jitterSet.length - 1].value);
        document.getElementById('stat-jitter').innerText = jitterVal.toFixed(2) + ' ms';
        var jitEl = document.getElementById('stat-jitter');
        jitEl.className = 'stat-value' + (jitterVal > 50 ? ' stat-bad' : jitterVal > 10 ? ' stat-warn' : ' stat-good');
    }
    if (lossSet.length > 0) {
        var lossVal = Math.round(Number(lossSet[lossSet.length - 1].value));
        document.getElementById('stat-loss').innerText = lossVal + '%';
        var lossEl = document.getElementById('stat-loss');
        lossEl.className = 'stat-value' + (lossVal > 10 ? ' stat-bad' : lossVal > 0 ? ' stat-warn' : ' stat-good');
    }

    // ========== GRAFICO: Latencia ==========
    if (latSet.length > 0) {
        var labels = latSet.map(function(e) { return formatarHora(e.clock); });
        var values = latSet.map(function(v) { return Number(v.value).toFixed(2); });
        var reduced = reduzirDados(labels, values, maxPontos);
        renderLineChart('lat', 'latencyChart', reduced.values, reduced.labels, '#58a6ff', 'rgba(88, 166, 255, 0.08)', false);
    }

    // ========== GRAFICO: Perda ==========
    if (lossSet.length > 0) {
        var labelsLoss = lossSet.map(function(e) { return formatarHora(e.clock); });
        var valuesLoss = lossSet.map(function(e) { return Math.round(Number(e.value)); });
        var reducedLoss = reduzirDados(labelsLoss, valuesLoss, maxPontos);
        renderLineChart('his', 'icmpChart', reducedLoss.values, reducedLoss.labels, '#f85149', 'rgba(248, 81, 73, 0.08)', true);
        renderGaugeChart(reducedLoss.values[reducedLoss.values.length - 1]);
    }

    // ========== GRAFICO: Jitter ==========
    if (jitterSet.length > 0) {
        var labelsJit = jitterSet.map(function(e) { return formatarHora(e.clock); });
        var valuesJit = jitterSet.map(function(e) { return Number(e.value).toFixed(2); });
        var reducedJit = reduzirDados(labelsJit, valuesJit, maxPontos);
        renderLineChart('jit', 'jitterChart', reducedJit.values, reducedJit.labels, '#bc8cff', 'rgba(188, 140, 255, 0.08)', false);
    }

    // ========== GRAFICO: Min/Max Range ==========
    if (minSet.length > 0 && maxSet.length > 0) {
        var points = Math.min(minSet.length, maxSet.length, maxPontos);
        var labelsRange = minSet.map(function(e) { return formatarHora(e.clock); });
        var reducedRangeMin = reduzirDados(labelsRange, minSet.map(function(e) { return Number(e.value).toFixed(2); }), points);
        var reducedRangeMax = reduzirDados(labelsRange, maxSet.map(function(e) { return Number(e.value).toFixed(2); }), points);
        renderRangeChart(reducedRangeMin.labels, reducedRangeMin.values, reducedRangeMax.values);
    }
}

function reduzirDados(labels, values, maxPoints) {
    if (labels.length <= maxPoints) {
        return { labels: labels, values: values };
    }

    var step = Math.ceil(labels.length / maxPoints);
    var reducedLabels = [];
    var reducedValues = [];

    for (var i = 0; i < labels.length; i += step) {
        reducedLabels.push(labels[i]);
        reducedValues.push(values[i]);
    }

    if (reducedLabels[reducedLabels.length - 1] !== labels[labels.length - 1]) {
        reducedLabels.push(labels[labels.length - 1]);
        reducedValues.push(values[values.length - 1]);
    }

    return { labels: reducedLabels, values: reducedValues };
}

function formatarHora(timestamp) {
    var date = new Date(timestamp * 1000);
    var data = date.toLocaleDateString('pt-BR', {
        timeZone: 'America/Sao_Paulo',
        day: '2-digit', month: '2-digit'
    });
    var hora = date.toLocaleTimeString('pt-BR', {
        timeZone: 'America/Sao_Paulo',
        hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
    });
    return data + ' ' + hora;
}

function setCanvasWidth(canvasId, labels) {
    var canvas = document.getElementById(canvasId);
    if (!canvas) return;

    if (horasFiltro >= 12) {
        var targetWidth = Math.max(labels.length * 40, 800);
        canvas.style.width = targetWidth + 'px';
    } else {
        canvas.style.width = '100%';
    }
}

function renderLineChart(key, canvasId, newData, newLabels, color, fillColor, isPercentage) {
    setCanvasWidth(canvasId, newLabels);
    var ctx = document.getElementById(canvasId).getContext('2d');

    if (charts[key]) {
        charts[key].data.labels = newLabels;
        charts[key].data.datasets[0].data = newData;
        charts[key].update('none');
    } else {
        charts[key] = new Chart(ctx, {
            type: 'line',
            data: {
                labels: newLabels,
                datasets: [{
                    data: newData,
                    borderColor: color,
                    backgroundColor: fillColor,
                    fill: true,
                    tension: 0.35,
                    pointRadius: function(context) {
                        return context.dataIndex === context.dataset.data.length - 1 ? 4 : 1.5;
                    },
                    pointBackgroundColor: function(context) {
                        return context.dataIndex === context.dataset.data.length - 1 ? '#fff5a8' : color;
                    },
                    pointBorderColor: function(context) {
                        return context.dataIndex === context.dataset.data.length - 1 ? '#ffd01f' : color;
                    },
                    pointHoverRadius: 6,
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                interaction: { intersect: false, mode: 'index' },
                scales: {
                    x: {
                        display: true,
                        grid: { display: false },
                        ticks: { color: '#8b949e', font: { size: 9 }, maxRotation: 45, minRotation: 30, autoSkip: true, maxTicksLimit: 10 }
                    },
                    y: {
                        beginAtZero: true,
                        min: isPercentage ? 0 : undefined,
                        max: isPercentage ? 100 : undefined,
                        grid: { color: 'rgba(48,54,61,0.5)' },
                        ticks: { color: '#8b949e', font: { size: 9 } }
                    }
                },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: '#21262d',
                        borderColor: '#30363d',
                        borderWidth: 1,
                        titleColor: '#c9d1d9',
                        bodyColor: '#c9d1d9',
                        titleFont: { size: 10 },
                        bodyFont: { size: 11 },
                        padding: 8,
                        callbacks: {
                            label: function(ctx) {
                                return isPercentage ? ctx.parsed.y + '%' : ctx.parsed.y + ' ms';
                            }
                        }
                    }
                }
            }
        });
    }
}

function renderRangeChart(labels, minData, maxData) {
    setCanvasWidth('rangeChart', labels);
    var ctx = document.getElementById('rangeChart').getContext('2d');

    if (charts.rng) {
        charts.rng.data.labels = labels;
        charts.rng.data.datasets[0].data = maxData;
        charts.rng.data.datasets[1].data = minData;
        charts.rng.update('none');
    } else {
        charts.rng = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: 'Max',
                        data: maxData,
                        borderColor: '#d29922',
                        backgroundColor: 'rgba(210, 153, 34, 0.12)',
                        fill: true,
                        tension: 0.35,
                        pointRadius: 2,
                        pointBackgroundColor: '#d29922',
                        borderWidth: 2
                    },
                    {
                        label: 'Min',
                        data: minData,
                        borderColor: '#3fb950',
                        backgroundColor: 'rgba(63, 185, 80, 0.12)',
                        fill: true,
                        tension: 0.35,
                        pointRadius: 2,
                        pointBackgroundColor: '#3fb950',
                        borderWidth: 2
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                interaction: { intersect: false, mode: 'index' },
                scales: {
                    x: {
                        display: true,
                        grid: { display: false },
                        ticks: { color: '#8b949e', font: { size: 9 }, maxRotation: 45, minRotation: 30, autoSkip: true, maxTicksLimit: 10 }
                    },
                    y: {
                        beginAtZero: true,
                        grid: { color: 'rgba(48,54,61,0.5)' },
                        ticks: { color: '#8b949e', font: { size: 9 } }
                    }
                },
                plugins: {
                    legend: {
                        display: true,
                        position: 'top',
                        labels: { color: '#8b949e', font: { size: 9 }, boxWidth: 10, padding: 8 }
                    },
                    tooltip: {
                        backgroundColor: '#21262d',
                        borderColor: '#30363d',
                        borderWidth: 1,
                        titleColor: '#c9d1d9',
                        bodyColor: '#c9d1d9',
                        callbacks: {
                            label: function(ctx) { return ctx.dataset.label + ': ' + ctx.parsed.y + ' ms'; }
                        }
                    }
                }
            }
        });
    }
}

function renderGaugeChart(percent) {
    var ctx = document.getElementById('gaugeChart').getContext('2d');
    var p = Math.round(percent);
    var color = p > 10 ? '#f85149' : p > 0 ? '#d29922' : '#3fb950';

    if (charts.gau) {
        charts.gau.data.datasets[0].data = [p, 100 - p];
        charts.gau.data.datasets[0].backgroundColor = [color, '#21262d'];
        charts.gau.update('none');
    } else {
        charts.gau = new Chart(ctx, {
            type: 'doughnut',
            data: {
                datasets: [{
                    data: [p, 100 - p],
                    backgroundColor: [color, '#21262d'],
                    borderWidth: 0
                }]
            },
            options: {
                rotation: -90,
                circumference: 180,
                cutout: '78%',
                animation: false,
                events: []
            }
        });
    }
    document.getElementById('percentLabel').innerText = p + '%';
    document.getElementById('percentLabel').style.color = color;
}

window.addEventListener('load', fetchDados);
