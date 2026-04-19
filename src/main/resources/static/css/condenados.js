var AUTH_TOKEN = localStorage.getItem('auth_token') || '';
var lista = [];

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

function escapeHtml(s) {
    if (s === null || s === undefined) return '';
    return String(s).replace(/[&<>"']/g, function(c){
        return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];
    });
}

function carregar() {
    fetch('/equipamentos-condenados', { headers: authHeaders() })
        .then(function(r){ if(!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
        .then(function(d){ lista = d || []; aplicarFiltros(); })
        .catch(function(e){ toast('Falha ao carregar: ' + e.message, 'error'); });
}

function aplicarFiltros() {
    var p = (document.getElementById('f-patrimonio').value || '').toLowerCase();
    var s = (document.getElementById('f-setor').value || '').toLowerCase();
    var f = (document.getElementById('f-filial').value || '').toLowerCase();
    var t = (document.getElementById('f-tecnico').value || '').toLowerCase();

    var filtrados = lista.filter(function(c){
        return (!p || (c.patrimonio||'').toLowerCase().indexOf(p) >= 0)
            && (!s || (c.setor||'').toLowerCase().indexOf(s) >= 0)
            && (!f || (c.filial||'').toLowerCase().indexOf(f) >= 0)
            && (!t || (c.condenadoPor||'').toLowerCase().indexOf(t) >= 0);
    });
    render(filtrados);
}

function render(itens) {
    var tb = document.getElementById('cond-tbody');
    var tecnicos = {};
    var mesAtual = new Date().getMonth();
    var anoAtual = new Date().getFullYear();
    var qtdMes = 0;

    tb.innerHTML = itens.map(function(c){
        if (c.condenadoPor) tecnicos[c.condenadoPor] = true;
        if (c.dataCondenacao) {
            var partes = c.dataCondenacao.split(' ')[0].split('/');
            if (partes.length === 3) {
                if (parseInt(partes[1]) - 1 === mesAtual && parseInt(partes[2]) === anoAtual) qtdMes++;
            }
        }
        return '<tr>' +
            '<td>' + escapeHtml(c.patrimonio) + '</td>' +
            '<td>' + escapeHtml(c.hostname) + '</td>' +
            '<td>' + escapeHtml((c.marca || '') + ' ' + (c.modelo || '')) + '</td>' +
            '<td>' + escapeHtml(c.setor) + '</td>' +
            '<td>' + escapeHtml(c.filial) + '</td>' +
            '<td>' + (c.totalManutencoes || 0) + '</td>' +
            '<td>' + escapeHtml(c.condenadoPor) + '</td>' +
            '<td>' + escapeHtml(c.dataCondenacao) + '</td>' +
            '<td><button class="btn" onclick="verDetalhe(' + c.id + ')">Detalhes</button></td>' +
        '</tr>';
    }).join('');

    document.getElementById('sum-total').textContent = itens.length;
    document.getElementById('sum-mes').textContent = qtdMes;
    document.getElementById('sum-tec').textContent = Object.keys(tecnicos).length;
}

function verDetalhe(id) {
    fetch('/equipamentos-condenados/' + id, { headers: authHeaders() })
        .then(function(r){ return r.json(); })
        .then(function(d){
            var historico = (d.historicoManutencoes || []).map(function(h){
                return '<div class="historico-item">' +
                    '<strong>' + escapeHtml(h.data) + '</strong> - ' +
                    'Tecnico: ' + escapeHtml(h.tecnico) + '<br>' +
                    'Descricao: ' + escapeHtml(h.descricao || '-') + '<br>' +
                    'Laudo: ' + escapeHtml(h.laudo || '-') +
                '</div>';
            }).join('') || '<div style="color:#8b949e">Sem manutencoes registradas</div>';

            document.getElementById('det-body').innerHTML =
                '<div class="det-section"><h4>Equipamento</h4>' +
                '<div class="det-grid">' +
                  '<div><span>Patrimonio:</span>' + escapeHtml(d.patrimonio) + '</div>' +
                  '<div><span>Hostname:</span>' + escapeHtml(d.hostname) + '</div>' +
                  '<div><span>Marca:</span>' + escapeHtml(d.marca) + '</div>' +
                  '<div><span>Modelo:</span>' + escapeHtml(d.modelo) + '</div>' +
                  '<div><span>Serie:</span>' + escapeHtml(d.numeroSerie) + '</div>' +
                  '<div><span>Tipo:</span>' + escapeHtml(d.tipo) + '</div>' +
                  '<div><span>Setor:</span>' + escapeHtml(d.setor) + '</div>' +
                  '<div><span>Filial:</span>' + escapeHtml(d.filial) + '</div>' +
                '</div></div>' +
                '<div class="det-section"><h4>Condenacao</h4>' +
                '<div class="det-grid">' +
                  '<div><span>Motivo:</span>' + escapeHtml(d.motivoCondenacao) + '</div>' +
                  '<div><span>Total manutencoes:</span>' + (d.totalManutencoes || 0) + '</div>' +
                  '<div><span>Condenado por:</span>' + escapeHtml(d.condenadoPor) + '</div>' +
                  '<div><span>Data:</span>' + escapeHtml(d.dataCondenacao) + '</div>' +
                '</div>' +
                '<p style="margin-top:8px; font-size:12px; color:#c9d1d9">' +
                  '<strong>Laudo final:</strong> ' + escapeHtml(d.laudoFinal || '-') + '</p>' +
                '</div>' +
                '<div class="det-section"><h4>Historico de manutencoes</h4>' + historico + '</div>';
            document.getElementById('modal-detalhe').classList.add('show');
        })
        .catch(function(e){ toast('Erro: ' + e.message, 'error'); });
}

function fecharDetalhe() {
    document.getElementById('modal-detalhe').classList.remove('show');
}

document.addEventListener('DOMContentLoaded', carregar);
