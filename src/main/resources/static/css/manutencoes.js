var AUTH_TOKEN = localStorage.getItem('auth_token') || '';
var lista = [];
var filtroAtual = { status: '', setor: '', responsavel: '', busca: '' };
var mntSelecionada = null;

function authHeaders(extra) {
    var h = extra || {};
    if (AUTH_TOKEN) h['Authorization'] = AUTH_TOKEN;
    return h;
}

function toast(msg, tipo) {
    var t = document.getElementById('toast');
    t.className = 'popup-toast show ' + (tipo || '');
    t.innerText = msg;
    setTimeout(function(){ t.className = 'popup-toast'; }, 3200);
}

function carregar() {
    var params = new URLSearchParams();
    if (filtroAtual.status) params.set('status', filtroAtual.status);
    if (filtroAtual.setor) params.set('setor', filtroAtual.setor);
    if (filtroAtual.responsavel) params.set('responsavel', filtroAtual.responsavel);
    fetch('/manutencoes/list?' + params.toString(), { headers: authHeaders() })
        .then(function(r){ if(!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
        .then(function(d){ lista = d || []; render(); })
        .catch(function(e){ toast('Falha ao carregar: ' + e.message, 'error'); });
}

function aplicarFiltros() {
    filtroAtual.status = document.getElementById('f-status').value || '';
    filtroAtual.setor = document.getElementById('f-setor').value || '';
    filtroAtual.responsavel = document.getElementById('f-responsavel').value || '';
    filtroAtual.busca = (document.getElementById('f-busca').value || '').toLowerCase();
    carregar();
}

function render() {
    var tb = document.getElementById('mnt-tbody');
    var itens = lista;
    if (filtroAtual.busca) {
        itens = itens.filter(function(m){
            var q = filtroAtual.busca;
            return (m.hostname||'').toLowerCase().indexOf(q) >= 0
                || (m.patrimonio||'').toLowerCase().indexOf(q) >= 0
                || (m.responsavelNome||'').toLowerCase().indexOf(q) >= 0
                || (m.responsavelUsername||'').toLowerCase().indexOf(q) >= 0;
        });
    }
    var alertas = 0, aguardando = 0, pendente = 0, pecas = 0;
    tb.innerHTML = itens.map(function(m){
        if (m.alerta) alertas++;
        if (m.status === 'AGUARDANDO_MANUTENCAO') aguardando++;
        if (m.status === 'PENDENTE') pendente++;
        if (m.status === 'AGUARDANDO_PECAS') pecas++;
        var tempo = m.duracaoDias > 0 ? (m.duracaoDias + 'd ' + (m.duracaoHoras % 24) + 'h')
                                      : (m.duracaoHoras + 'h');
        var classeLinha = m.alerta ? 'row-alerta' : '';
        var classeTempo = m.alerta ? 'tempo-alerta' : '';
        var host = m.hostname || m.patrimonio || '-';
        var inicial = String(host).trim().charAt(0).toUpperCase() || '?';
        var responsavel = m.responsavelNome || m.responsavelUsername || '-';
        var respIni = String(responsavel).trim().charAt(0).toUpperCase() || '?';
        var statusLabel = String(m.status || '').replace(/_/g, ' ');
        return '<tr class="' + classeLinha + '">' +
            '<td>' +
                '<div class="equip-cell">' +
                    '<div class="equip-avatar">' + escapeHtml(inicial) + '</div>' +
                    '<div class="equip-info">' +
                        '<div class="equip-host">' + escapeHtml(host) + '</div>' +
                        '<div class="equip-sub">' + escapeHtml(m.agentId || '') + '</div>' +
                    '</div>' +
                '</div>' +
            '</td>' +
            '<td>' + escapeHtml(m.patrimonio) + '</td>' +
            '<td>' + escapeHtml(m.setor) + '</td>' +
            '<td>' + escapeHtml(m.filial) + '</td>' +
            '<td><span class="tipo tipo-' + escapeHtml(m.tipoManutencao || 'INDEF') + '">' + escapeHtml(m.tipoManutencao || '-') + '</span></td>' +
            '<td><span class="badge badge-' + m.status + '">' + escapeHtml(statusLabel) + '</span></td>' +
            '<td>' +
                '<span class="user-pill">' +
                    '<span class="user-avatar">' + escapeHtml(respIni) + '</span>' +
                    '<span class="user-name">' + escapeHtml(responsavel) + '</span>' +
                '</span>' +
            '</td>' +
            '<td>' + escapeHtml(m.dataEntrada) + '</td>' +
            '<td class="' + classeTempo + '">' + escapeHtml(tempo) + (m.alerta ? ' <span class="alert-icon" title="Mais de 2 dias">⚠</span>' : '') + '</td>' +
            '<td class="col-acoes">' +
                '<button class="icon-btn icon-btn-det" title="Detalhes" onclick="verDetalhes(' + m.id + ')">' +
                    '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 8v.01"/><path d="M11 12h1v4h1"/></svg>' +
                '</button>' +
                '<button class="icon-btn icon-btn-status" title="Alterar status" onclick="abrirStatus(' + m.id + ')">' +
                    '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 1 1 3 3L7 19l-4 1 1-4Z"/></svg>' +
                '</button>' +
                '<button class="icon-btn icon-btn-hist" title="Historico" onclick="verHistorico(' + m.id + ')">' +
                    '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5"/><path d="M12 7v5l3 2"/></svg>' +
                '</button>' +
            '</td>' +
        '</tr>';
    }).join('');
    document.getElementById('sum-total').textContent = itens.length;
    document.getElementById('sum-aguardando').textContent = aguardando;
    document.getElementById('sum-pendentes').textContent = pendente;
    document.getElementById('sum-pecas').textContent = pecas;
    document.getElementById('sum-alertas').textContent = alertas;
}

function escapeHtml(s) {
    if (s === null || s === undefined) return '';
    return String(s).replace(/[&<>"']/g, function(c){
        return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];
    });
}

function abrirStatus(id) {
    mntSelecionada = id;
    var item = lista.find(function(x){ return x.id === id; }) || {};
    var atual = item.status || '';
    var proximo = proximoStatusSugerido(atual);
    var titulo = '#' + id + (atual ? ' (atual: ' + atual.replace(/_/g,' ') + ')' : '');
    document.getElementById('mnt-id').textContent = titulo;
    document.getElementById('ns-status').value = proximo;
    document.getElementById('ns-obs').value = '';
    document.getElementById('ns-user').value = '';
    document.getElementById('ns-pass').value = '';
    document.getElementById('ns-laudo').value = '';
    toggleLaudo();
    document.getElementById('modal-status').classList.add('show');
}

function proximoStatusSugerido(atual) {
    switch (atual) {
        case 'AGUARDANDO_MANUTENCAO': return 'PENDENTE';
        case 'PENDENTE':              return 'EM_ANALISE';
        case 'AGUARDANDO_PECAS':      return 'EM_ANALISE';
        case 'EM_ANALISE':            return 'FINALIZADO';
        case 'FINALIZADO':            return 'ENTREGUE';
        default:                      return 'PENDENTE';
    }
}

function fecharModalStatus() {
    document.getElementById('modal-status').classList.remove('show');
    mntSelecionada = null;
}

function toggleLaudo() {
    var v = document.getElementById('ns-status').value;
    document.getElementById('row-laudo').style.display = (v === 'CONDENADO') ? '' : 'none';
}

document.addEventListener('DOMContentLoaded', function(){
    document.getElementById('ns-status').addEventListener('change', toggleLaudo);
    carregar();
});

function confirmarStatus() {
    if (!mntSelecionada) return;
    var status = document.getElementById('ns-status').value;
    var obs = document.getElementById('ns-obs').value;
    var user = document.getElementById('ns-user').value;
    var pass = document.getElementById('ns-pass').value;
    if (!user || !pass) { toast('Informe usuario e senha', 'error'); return; }

    var form = new FormData();
    form.append('novoStatus', status);
    form.append('usuario', user);
    form.append('senha', pass);
    if (obs) form.append('observacao', obs);
    if (status === 'CONDENADO') {
        var f = document.getElementById('ns-laudo').files[0];
        if (!f) { toast('Anexe o laudo tecnico', 'error'); return; }
        form.append('laudo', f);
    }

    fetch('/manutencoes/' + mntSelecionada + '/status', {
        method: 'POST', headers: authHeaders(), body: form
    }).then(function(r){ return r.json().then(function(j){ return { ok: r.ok, body: j }; }); })
      .then(function(res){
          if (!res.ok) { toast(res.body.message || 'Falha', 'error'); return; }
          toast('Status alterado: ' + res.body.statusNovo);
          fecharModalStatus();
          carregar();
      })
      .catch(function(e){ toast('Erro: ' + e.message, 'error'); });
}

function abrirPdf(forceDownload) {
    var params = new URLSearchParams();
    if (filtroAtual.status) params.set('status', filtroAtual.status);
    if (filtroAtual.setor) params.set('setor', filtroAtual.setor);
    if (filtroAtual.responsavel) params.set('responsavel', filtroAtual.responsavel);
    if (forceDownload) params.set('download', '1');
    fetch('/manutencoes/relatorio.pdf?' + params.toString(), { headers: authHeaders() })
        .then(function(r){ if (!r.ok) throw new Error('PDF falhou'); return r.blob(); })
        .then(function(blob){
            var url = URL.createObjectURL(blob);
            if (forceDownload) {
                var a = document.createElement('a');
                a.href = url; a.download = 'manutencoes.pdf';
                document.body.appendChild(a); a.click(); a.remove();
            } else {
                window.open(url, '_blank');
            }
            setTimeout(function(){ URL.revokeObjectURL(url); }, 30000);
        })
        .catch(function(e){ toast('Erro: ' + e.message, 'error'); });
}
function visualizarPdf() { abrirPdf(false); }
function baixarPdf() { abrirPdf(true); }

// ===== DETALHES DA MANUTENCAO + EQUIPAMENTO =====
function verDetalhes(id) {
    fetch('/manutencoes/' + id + '/detalhe', { headers: authHeaders() })
        .then(function(r){ if(!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
        .then(function(d){ renderDetalhes(d); })
        .catch(function(e){ toast('Erro ao carregar: ' + e.message, 'error'); });
}

function renderDetalhes(d) {
    var eq = d.equipamento || {};
    var m = d.manutencao || {};
    var hist = Array.isArray(d.historico) ? d.historico : [];

    document.getElementById('det-titulo').textContent =
        'Detalhes: ' + (eq.hostname || eq.agentId || m.hostname || ('#' + m.id));

    // Imagem principal do equipamento
    var imgBox = document.getElementById('det-equip-img');
    if (eq.temImagem && eq.agentId) {
        var imgUrl = '/manutencoes/agente/' + encodeURIComponent(eq.agentId) + '/imagem?t=' + Date.now();
        imgBox.innerHTML = '<img src="' + imgUrl + '" alt="Equipamento">';
        imgBox.onclick = function(){ abrirImgViewer(imgUrl); };
        imgBox.classList.add('has-img');
    } else {
        imgBox.innerHTML = '<span class="det-empty">Sem imagem</span>';
        imgBox.onclick = null;
        imgBox.classList.remove('has-img');
    }

    // Info do equipamento
    var items = [
        ['Hostname', eq.hostname],
        ['Agent ID', eq.agentId],
        ['Patrimonio', eq.patrimonio],
        ['Tipo', eq.tipo],
        ['Marca', eq.marca],
        ['Modelo', eq.modelo],
        ['N. Serie', eq.numeroSerie],
        ['Setor', eq.setor],
        ['Filial', eq.filial],
        ['IP', eq.ip],
        ['AnyDesk', eq.anydesk],
        ['Dominio', eq.dominio],
        ['Status', eq.status, eq.status === 'ONLINE' ? 'ok' : 'bad'],
        ['Status Uso', eq.statusUso || 'NORMAL', classUso(eq.statusUso)],
        ['Total Manutencoes', (eq.numManutencoes != null ? eq.numManutencoes : 0), countCls(eq.numManutencoes)]
    ];
    document.getElementById('det-equip-info').innerHTML = items.map(function(it){
        var val = (it[1] == null || it[1] === '') ? '--' : it[1];
        var cls = it[2] || '';
        return '<div class="det-info-item"><span class="det-info-label">' + escapeHtml(it[0]) + '</span>' +
               '<span class="det-info-value ' + cls + '">' + escapeHtml(String(val)) + '</span></div>';
    }).join('');

    // Info desta manutencao
    document.getElementById('det-manut-info').innerHTML = renderItemManutencao(m, true);

    // Historico das manutencoes do equipamento
    document.getElementById('det-hist-count').textContent = hist.length;
    var box = document.getElementById('det-hist-list');
    if (!hist.length) {
        box.innerHTML = '<div class="det-empty-block">Sem historico de manutencoes</div>';
    } else {
        box.innerHTML = hist.map(function(h){ return renderItemManutencao(h, h.id === m.id); }).join('');
    }

    trocarDetTab('equip');
    document.getElementById('modal-detalhe').classList.add('show');
}

function renderItemManutencao(m, destacar) {
    if (!m || !m.id) return '';
    var statusCls = 'badge-' + (m.status || 'INDEF');
    var tipoCls = 'tipo-' + (m.tipoManutencao || 'INDEF');
    var acoes = '';
    var thumbs = '';
    if (m.temImagem) {
        var imgUrl = '/manutencoes/' + m.id + '/imagem?t=' + Date.now();
        thumbs += '<a class="manut-thumb" onclick="abrirImgViewer(\'' + imgUrl + '\')">' +
                  '<img src="' + imgUrl + '" alt="imagem"></a>';
    }
    if (m.temAnexo) {
        acoes += '<button class="btn btn-sm" onclick="window.open(\'/manutencoes/' + m.id + '/arquivo\',\'_blank\')">Ver Laudo</button>';
        acoes += '<button class="btn btn-sm" onclick="window.open(\'/manutencoes/' + m.id + '/download\',\'_blank\')">Baixar</button>';
    }
    var resp = m.responsavelNome || m.responsavelUsername || '--';
    var tec = m.tecnicoNome || m.tecnicoUsername || '--';
    return '<div class="det-manut ' + (destacar ? 'highlight' : '') + '">' +
        '<div class="det-manut-head">' +
            '<div class="det-manut-title">' +
                'Manutencao #' + m.id +
                ' <span class="badge ' + statusCls + '">' + escapeHtml(String(m.status || '').replace(/_/g,' ')) + '</span>' +
                ' <span class="tipo ' + tipoCls + '">' + escapeHtml(m.tipoManutencao || '-') + '</span>' +
            '</div>' +
            '<div class="det-manut-date">' + escapeHtml(m.dataEntrada || m.data || '') + '</div>' +
        '</div>' +
        '<div class="det-manut-people">' +
            '<div><span class="lbl">Tecnico (criou):</span> ' + escapeHtml(tec) + '</div>' +
            '<div><span class="lbl">Responsavel atual:</span> ' + escapeHtml(resp) + '</div>' +
        '</div>' +
        (m.descricao ? '<div class="det-manut-desc"><span class="lbl">Descricao:</span> ' + escapeHtml(m.descricao) + '</div>' : '') +
        (m.laudoFinal ? '<div class="det-manut-laudo"><span class="lbl">Laudo Final:</span><div class="laudo-body">' + escapeHtml(m.laudoFinal) + '</div></div>' : '') +
        (thumbs ? '<div class="manut-thumbs">' + thumbs + '</div>' : '') +
        (acoes ? '<div class="det-manut-actions">' + acoes + '</div>' : '') +
    '</div>';
}

function classUso(s) {
    if (s === 'CRITICO') return 'bad';
    if (s === 'USADO') return 'warn';
    return 'ok';
}
function countCls(n) {
    var v = Number(n) || 0;
    if (v >= 10) return 'bad';
    if (v >= 5) return 'warn';
    if (v > 0) return 'ok';
    return '';
}

function trocarDetTab(tab) {
    document.querySelectorAll('.det-tab').forEach(function(t){
        t.classList.toggle('active', t.dataset.tab === tab);
    });
    document.querySelectorAll('.det-pane').forEach(function(p){
        p.classList.toggle('active', p.id === 'pane-' + tab);
    });
}

function fecharDetalhe() {
    document.getElementById('modal-detalhe').classList.remove('show');
}

// ===== LIGHTBOX =====
function abrirImgViewer(src) {
    if (!src) return;
    var v = document.getElementById('img-viewer');
    var img = document.getElementById('img-viewer-img');
    img.src = src;
    v.classList.add('show');
}
function fecharImgViewer(ev) {
    if (ev && ev.target && ev.target.tagName === 'IMG' && ev.target.id === 'img-viewer-img') return;
    var v = document.getElementById('img-viewer');
    if (v) v.classList.remove('show');
}
document.addEventListener('keydown', function(ev){
    if (ev.key === 'Escape') { fecharImgViewer(); fecharDetalhe(); }
});

function verHistorico(id) {
    fetch('/manutencoes/' + id + '/historico-status', { headers: authHeaders() })
        .then(function(r){ return r.json(); })
        .then(function(logs){
            if (!logs || !logs.length) { toast('Sem historico de status'); return; }
            var msg = logs.map(function(l){
                return '[' + l.dataHora + '] ' + (l.statusAnterior || 'inicio')
                     + ' → ' + l.statusNovo + ' por ' + (l.usuarioNome || l.usuario);
            }).join('\n');
            alert(msg);
        })
        .catch(function(e){ toast('Erro: ' + e.message, 'error'); });
}
