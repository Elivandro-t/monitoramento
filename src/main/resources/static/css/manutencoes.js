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
    document.getElementById('mnt-id').textContent = '#' + id;
    document.getElementById('ns-status').value = 'PENDENTE';
    document.getElementById('ns-obs').value = '';
    document.getElementById('ns-user').value = '';
    document.getElementById('ns-pass').value = '';
    document.getElementById('ns-laudo').value = '';
    toggleLaudo();
    document.getElementById('modal-status').classList.add('show');
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
