var AUTH_TOKEN = localStorage.getItem('auth_token') || '';
var todosEquipamentos = [];
var filiaisCache = [];
var agenteSelecionado = null;

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

function abrirModal(id) {
    var m = document.getElementById(id);
    if (m) m.classList.add('show');
}
function fecharModal(id) {
    var m = document.getElementById(id);
    if (m) m.classList.remove('show');
}

// ===== LISTAR =====
function carregar() {
    return fetch('/agentes', { headers: authHeaders() })
        .then(function(r){
            if (r.status === 403) { toast('Sem permissao. Faca login.', 'err'); return []; }
            return r.json();
        })
        .then(function(data){
            todosEquipamentos = Array.isArray(data) ? data : [];
            aplicarFiltros();
            atualizarResumo();
        })
        .catch(function(){ toast('Erro ao carregar equipamentos', 'err'); });
}

function carregarFiliais() {
    return fetch('/filiais')
        .then(function(r){ return r.json(); })
        .then(function(data){
            filiaisCache = Array.isArray(data) ? data : [];
            // popula selects
            var selFiltro = document.getElementById('f-filial');
            var selEq = document.getElementById('eq-filial');
            selFiltro.innerHTML = '<option value="">Todas</option>';
            selEq.innerHTML = '<option value="">--</option>';
            filiaisCache.forEach(function(f){
                var o1 = document.createElement('option'); o1.value = f.nome; o1.textContent = f.nome;
                selFiltro.appendChild(o1);
                var o2 = document.createElement('option'); o2.value = f.nome; o2.textContent = f.nome;
                selEq.appendChild(o2);
            });
            atualizarListaFiliais();
        })
        .catch(function(){});
}

// ===== RESUMO =====
function atualizarResumo() {
    var total = todosEquipamentos.length;
    var online = 0, offline = 0, usados = 0, criticos = 0, venc = 0;
    todosEquipamentos.forEach(function(a){
        if (a.status === 'ONLINE') online++; else offline++;
        if (a.statusUso === 'USADO') usados++;
        if (a.statusUso === 'CRITICO') criticos++;
        if (a.garantiaVencida) venc++;
    });
    document.getElementById('sum-total').innerText = total;
    document.getElementById('sum-online').innerText = online;
    document.getElementById('sum-offline').innerText = offline;
    document.getElementById('sum-usados').innerText = usados;
    document.getElementById('sum-criticos').innerText = criticos;
    document.getElementById('sum-gar-vencida').innerText = venc;
}

// ===== FILTROS =====
function aplicarFiltros() {
    var busca = (document.getElementById('f-busca').value || '').toLowerCase().trim();
    var setor = document.getElementById('f-setor').value;
    var filial = document.getElementById('f-filial').value;
    var status = document.getElementById('f-status').value;
    var uso = document.getElementById('f-uso').value;
    var tipo = document.getElementById('f-tipo').value;

    var lista = todosEquipamentos.filter(function(a){
        if (setor && (a.setor || '').toUpperCase() !== setor.toUpperCase()) return false;
        if (filial && (a.filial || '') !== filial) return false;
        if (status && (a.status || '') !== status) return false;
        if (uso && (a.statusUso || '') !== uso) return false;
        if (tipo && (a.tipo || '').toUpperCase() !== tipo.toUpperCase()) return false;
        if (busca) {
            var hay = [a.hostname, a.agentId, a.ip, a.patrimonio, a.anydesk, a.marca, a.modelo, a.numeroSerie]
                        .map(function(x){ return (x||'').toString().toLowerCase(); }).join(' ');
            if (hay.indexOf(busca) === -1) return false;
        }
        return true;
    });
    renderTabela(lista);
}

function limparFiltros() {
    ['f-busca','f-setor','f-filial','f-status','f-uso','f-tipo'].forEach(function(id){
        var el = document.getElementById(id); if (el) el.value = '';
    });
    aplicarFiltros();
}

// ===== TABELA =====
function metricBar(v, label) {
    var val = Number(v) || 0;
    var clazz = val >= 85 ? 'bar-high' : (val >= 60 ? 'bar-mid' : 'bar-low');
    return '<div class="metric-bar-wrap">' +
              '<span class="metric-bar-label">' + val.toFixed(1) + '%</span>' +
              '<div class="metric-bar"><div class="metric-bar-fill ' + clazz + '" style="width:' + Math.min(val,100) + '%"></div></div>' +
           '</div>';
}

function renderTabela(lista) {
    var tb = document.getElementById('tbody-equip');
    if (!lista.length) {
        tb.innerHTML = '<tr><td colspan="11"><div class="empty-state">Nenhum equipamento encontrado</div></td></tr>';
        return;
    }
    var html = '';
    lista.forEach(function(a){
        var statusClass = a.status === 'ONLINE' ? 'status-online' : 'status-offline';
        var tipoLabel = a.tipo || '--';
        var manutChip = a.numManutencoes > 0 ? '<strong>' + a.numManutencoes + '</strong>' : '0';
        html +=
        '<tr>' +
            '<td>' +
                '<div class="cell-hostname">' + esc(a.hostname || a.agentId) + '</div>' +
                '<div class="cell-muted">' + esc(a.agentId) + '</div>' +
            '</td>' +
            '<td>' + esc(tipoLabel) + '</td>' +
            '<td>' +
                '<div>' + esc(a.ip || '---') + '</div>' +
                '<div class="cell-muted">AD: ' + esc(a.anydesk || '---') + '</div>' +
            '</td>' +
            '<td>' +
                '<div>' + esc(a.setor || '---') + '</div>' +
                '<div class="cell-muted">' + esc(a.filial || '---') + '</div>' +
            '</td>' +
            '<td>' + metricBar(a.cpu) + '</td>' +
            '<td>' + metricBar(a.memoriaPercent) + '</td>' +
            '<td>' + metricBar(a.discoPercent) + '</td>' +
            '<td><span class="status-chip ' + statusClass + '"><span class="status-dot"></span>' + esc(a.status || '--') + '</span></td>' +
            '<td><span class="uso-chip uso-' + (a.statusUso || 'NORMAL') + '">' + (a.statusUso || 'NORMAL') + '</span></td>' +
            '<td>' + manutChip + '</td>' +
            '<td class="row-actions">' +
                '<button class="btn" onclick="verDetalhes(' + a.id + ')">Detalhes</button>' +
                '<button class="btn btn-warn" onclick="editarEquip(' + a.id + ')">Editar</button>' +
                '<button class="btn btn-danger" onclick="deletarEquip(' + a.id + ', \'' + esc(a.hostname || a.agentId) + '\')">Del</button>' +
            '</td>' +
        '</tr>';
    });
    document.getElementById('tbody-equip').innerHTML = html;
}

function esc(s) {
    if (s === null || s === undefined) return '';
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

// ===== CRUD EQUIP =====
function abrirModalNovo() {
    document.getElementById('modal-equip-titulo').innerText = 'Novo Equipamento';
    document.getElementById('eq-id').value = '';
    ['eq-agentId','eq-hostname','eq-patrimonio','eq-marca','eq-modelo','eq-sn',
     'eq-anydesk','eq-dominio','eq-dataCompra','eq-garantia','eq-descricao'].forEach(function(id){
        document.getElementById(id).value = '';
    });
    document.getElementById('eq-tipo').value = '';
    document.getElementById('eq-setor').value = '';
    document.getElementById('eq-filial').value = '';
    document.getElementById('eq-agentId').readOnly = false;
    abrirModal('modal-equip');
}

function editarEquip(id) {
    var a = todosEquipamentos.find(function(x){ return x.id === id; });
    if (!a) return;
    document.getElementById('modal-equip-titulo').innerText = 'Editar: ' + (a.hostname || a.agentId);
    document.getElementById('eq-id').value = a.id;
    document.getElementById('eq-agentId').value = a.agentId || '';
    document.getElementById('eq-agentId').readOnly = true;
    document.getElementById('eq-hostname').value = a.hostname || '';
    document.getElementById('eq-patrimonio').value = a.patrimonio || '';
    document.getElementById('eq-marca').value = a.marca || '';
    document.getElementById('eq-modelo').value = a.modelo || '';
    document.getElementById('eq-sn').value = a.numeroSerie || '';
    document.getElementById('eq-anydesk').value = a.anydesk || '';
    document.getElementById('eq-dominio').value = a.dominio || '';
    document.getElementById('eq-tipo').value = a.tipo || '';
    document.getElementById('eq-setor').value = a.setor || '';
    document.getElementById('eq-filial').value = a.filial || '';
    document.getElementById('eq-dataCompra').value = a.dataCompra || '';
    document.getElementById('eq-garantia').value = a.garantiaExpiracao || '';
    document.getElementById('eq-descricao').value = a.descricao || '';
    abrirModal('modal-equip');
}

function salvarEquip() {
    var id = document.getElementById('eq-id').value;
    var body = {
        agentId: document.getElementById('eq-agentId').value.trim(),
        hostname: document.getElementById('eq-hostname').value.trim(),
        patrimonio: document.getElementById('eq-patrimonio').value.trim(),
        marca: document.getElementById('eq-marca').value.trim(),
        modelo: document.getElementById('eq-modelo').value.trim(),
        numeroSerie: document.getElementById('eq-sn').value.trim(),
        anydesk: document.getElementById('eq-anydesk').value.trim(),
        dominio: document.getElementById('eq-dominio').value.trim(),
        tipo: document.getElementById('eq-tipo').value,
        setor: document.getElementById('eq-setor').value,
        filial: document.getElementById('eq-filial').value,
        dataCompra: document.getElementById('eq-dataCompra').value,
        garantiaExpiracao: document.getElementById('eq-garantia').value,
        descricao: document.getElementById('eq-descricao').value.trim()
    };
    if (!body.agentId) { toast('Agent ID e obrigatorio', 'err'); return; }

    var url = id ? '/agentes/' + id : '/agentes';
    var method = id ? 'PUT' : 'POST';
    fetch(url, {
        method: method,
        headers: authHeaders({'Content-Type':'application/json'}),
        body: JSON.stringify(body)
    })
    .then(function(r){ return r.json().then(function(d){ return {s: r.status, d: d}; }); })
    .then(function(res){
        if (res.s >= 200 && res.s < 300) {
            toast(res.d.message || 'Salvo com sucesso', 'ok');
            fecharModal('modal-equip');
            carregar();
        } else {
            toast(res.d.message || 'Erro ao salvar', 'err');
        }
    })
    .catch(function(){ toast('Erro de conexao', 'err'); });
}

function deletarEquip(id, nome) {
    if (!confirm('Excluir equipamento "' + nome + '"? Esta acao nao pode ser desfeita.')) return;
    fetch('/agentes/' + id, { method: 'DELETE', headers: authHeaders() })
    .then(function(r){ return r.json().then(function(d){ return {s:r.status, d:d}; }); })
    .then(function(res){
        if (res.s >= 200 && res.s < 300) { toast(res.d.message, 'ok'); carregar(); }
        else toast(res.d.message || 'Erro', 'err');
    });
}

// ===== DETALHES =====
function verDetalhes(id) {
    fetch('/agentes/' + id, { headers: authHeaders() })
        .then(function(r){ return r.json(); })
        .then(function(a){
            agenteSelecionado = a;
            document.getElementById('det-titulo').innerText = 'Detalhes: ' + (a.hostname || a.agentId);
            renderDetInfo(a);
            renderManutList(a.manutencoes || []);
            renderImagemBig(a);
            trocarTab('info');
            abrirModal('modal-detalhe');
        });
}

function renderDetInfo(a) {
    var statusGar = a.garantiaVencida ? 'bad' : (a.garantiaExpiracao ? 'ok' : '');
    var anos = a.anosUso;
    var statusUsoClass = a.statusUso === 'CRITICO' ? 'bad' : (a.statusUso === 'USADO' ? 'warn' : 'ok');
    var items = [
        ['Agent ID', a.agentId],
        ['Hostname', a.hostname],
        ['Tipo', a.tipo || '--'],
        ['Marca', a.marca || '--'],
        ['Modelo', a.modelo || '--'],
        ['Patrimonio', a.patrimonio || '--'],
        ['Numero Serie', a.numeroSerie || '--'],
        ['IP', a.ip || '--'],
        ['AnyDesk', a.anydesk || '--'],
        ['Dominio', a.dominio || '--'],
        ['Sistema Op.', a.so || '--'],
        ['Usuario', a.usuario || '--'],
        ['Setor', a.setor || '--'],
        ['Filial', a.filial || '--'],
        ['Data Compra', a.dataCompra || '--'],
        ['Anos de Uso', anos + (anos === 1 ? ' ano' : ' anos')],
        ['Expiracao Garantia', a.garantiaExpiracao || '--', statusGar],
        ['Garantia Vencida?', a.garantiaVencida ? 'SIM' : 'NAO', a.garantiaVencida ? 'bad' : 'ok'],
        ['Num. Manutencoes', a.numManutencoes || 0, a.numManutencoes >= 10 ? 'bad' : (a.numManutencoes >= 5 ? 'warn' : 'ok')],
        ['Status de Uso', a.statusUso || 'NORMAL', statusUsoClass],
        ['Status', a.status || '--', a.status === 'ONLINE' ? 'ok' : 'bad'],
        ['CPU', (a.cpu || 0).toFixed(1) + '%'],
        ['Memoria', (a.memoriaPercent || 0).toFixed(1) + '%'],
        ['Disco', (a.discoPercent || 0).toFixed(1) + '%'],
        ['Data Cadastro', a.dataCadastro || '--'],
        ['Ultimo Contato', a.ultimoContato || '--']
    ];
    var info = items.map(function(it){
        return '<div class="info-item"><span class="info-label">' + esc(it[0]) + '</span>' +
               '<span class="info-value ' + (it[2]||'') + '">' + esc(it[1]) + '</span></div>';
    }).join('');
    // Descricao
    if (a.descricao) {
        info += '<div class="info-item" style="grid-column:1/-1"><span class="info-label">Descricao</span>' +
                '<span class="info-value">' + esc(a.descricao) + '</span></div>';
    }
    document.getElementById('det-info').innerHTML = info;
}

function renderManutList(ms) {
    var box = document.getElementById('manut-list');
    if (!ms.length) { box.innerHTML = '<div class="empty-state">Nenhuma manutencao registrada</div>'; return; }
    box.innerHTML = ms.map(function(m){
        var acoes = '';
        if (m.anexo) {
            acoes += '<button class="btn" onclick="window.open(\'/manutencoes/' + m.id + '/arquivo\',\'_blank\')">Ver Anexo</button>';
            acoes += '<button class="btn" onclick="window.open(\'/manutencoes/' + m.id + '/download\',\'_blank\')">Baixar</button>';
        }
        if (m.imagem) {
            acoes += '<button class="btn" onclick="window.open(\'/manutencoes/' + m.id + '/imagem\',\'_blank\')">Ver Imagem</button>';
        }
        acoes += '<button class="btn btn-danger" onclick="deletarManut(' + m.id + ')">Excluir</button>';

        return '<div class="manut-item">' +
            '<div class="manut-header">' +
                '<div class="manut-tecnico">' + esc(m.tecnico || '--') + '</div>' +
                '<div class="manut-data">' + esc(m.data || '') + '</div>' +
            '</div>' +
            '<div class="manut-desc"><strong>Descricao:</strong> ' + esc(m.descricao || '--') + '</div>' +
            (m.laudo ? '<div class="manut-laudo">' + esc(m.laudo) + '</div>' : '') +
            (m.anexo ? '<div class="cell-muted">Anexo: ' + esc(m.anexo) + '</div>' : '') +
            '<div class="manut-actions">' + acoes + '</div>' +
        '</div>';
    }).join('');
}

function renderImagemBig(a) {
    var big = document.getElementById('img-big');
    var empty = document.getElementById('img-empty');
    if (a.imagemUrl) {
        big.src = '/manutencoes/agente/' + encodeURIComponent(a.agentId) + '/imagem?t=' + Date.now();
        big.style.display = 'inline-block';
        empty.style.display = 'none';
    } else {
        big.style.display = 'none';
        empty.style.display = 'block';
    }
    // Thumb na aba Info
    var box = document.getElementById('det-imagem-box');
    if (a.imagemUrl) {
        box.innerHTML = '<img src="/manutencoes/agente/' + encodeURIComponent(a.agentId) + '/imagem?t=' + Date.now() + '">';
    } else {
        box.innerHTML = '<span class="detail-image-empty">Sem imagem</span>';
    }
}

function trocarTab(tab) {
    document.querySelectorAll('.tab').forEach(function(t){ t.classList.toggle('active', t.dataset.tab === tab); });
    document.querySelectorAll('.tab-content').forEach(function(c){ c.classList.toggle('active', c.id === 'tab-' + tab); });
}

// ===== MANUTENCAO =====
function abrirModalManut() {
    if (!agenteSelecionado) return;
    document.getElementById('mn-descricao').value = '';
    document.getElementById('mn-laudo').value = '';
    document.getElementById('mn-arquivo').value = '';
    document.getElementById('mn-imagem').value = '';
    abrirModal('modal-manut');
}

function salvarManut() {
    if (!agenteSelecionado) return;
    var fd = new FormData();
    fd.append('descricao', document.getElementById('mn-descricao').value);
    fd.append('laudoFinal', document.getElementById('mn-laudo').value);
    var arq = document.getElementById('mn-arquivo').files[0];
    var img = document.getElementById('mn-imagem').files[0];
    if (arq) fd.append('arquivo', arq);
    if (img) fd.append('imagem', img);

    fetch('/manutencoes/agente/' + encodeURIComponent(agenteSelecionado.agentId), {
        method: 'POST',
        headers: authHeaders(),
        body: fd
    })
    .then(function(r){ return r.json().then(function(d){ return {s:r.status, d:d}; }); })
    .then(function(res){
        if (res.s >= 200 && res.s < 300) {
            toast('Manutencao registrada', 'ok');
            fecharModal('modal-manut');
            verDetalhes(agenteSelecionado.id);
            carregar();
        } else {
            toast(res.d.message || 'Erro ao registrar', 'err');
        }
    })
    .catch(function(){ toast('Erro de conexao', 'err'); });
}

function deletarManut(id) {
    if (!confirm('Excluir esta manutencao?')) return;
    fetch('/manutencoes/' + id, { method: 'DELETE', headers: authHeaders() })
        .then(function(r){ return r.json().then(function(d){ return {s:r.status, d:d}; }); })
        .then(function(res){
            if (res.s >= 200 && res.s < 300) {
                toast('Excluida', 'ok');
                if (agenteSelecionado) verDetalhes(agenteSelecionado.id);
                carregar();
            } else { toast(res.d.message || 'Erro', 'err'); }
        });
}

function uploadImagemPrincipal() {
    if (!agenteSelecionado) return;
    var f = document.getElementById('file-img').files[0];
    if (!f) return;
    var fd = new FormData();
    fd.append('imagem', f);
    fetch('/manutencoes/agente/' + encodeURIComponent(agenteSelecionado.agentId) + '/imagem', {
        method: 'POST', headers: authHeaders(), body: fd
    })
    .then(function(r){ return r.json().then(function(d){ return {s:r.status, d:d}; }); })
    .then(function(res){
        if (res.s >= 200 && res.s < 300) {
            toast('Imagem enviada', 'ok');
            agenteSelecionado.imagemUrl = res.d.imagemUrl;
            renderImagemBig(agenteSelecionado);
        } else { toast(res.d.message || 'Erro', 'err'); }
    });
}

// ===== FILIAIS =====
function abrirModalFiliais() { atualizarListaFiliais(); abrirModal('modal-filiais'); }

function atualizarListaFiliais() {
    var box = document.getElementById('lista-filiais');
    if (!box) return;
    if (!filiaisCache.length) { box.innerHTML = '<div class="empty-state">Nenhuma filial cadastrada</div>'; return; }
    box.innerHTML = filiaisCache.map(function(f){
        return '<div class="manut-item">' +
            '<div class="manut-header">' +
                '<div class="manut-tecnico">' + esc(f.nome) + ' <span class="cell-muted">(' + esc(f.codigo) + ')</span></div>' +
                '<div class="cell-muted">' + esc(f.cidade) + '/' + esc(f.estado) + '</div>' +
            '</div>' +
            '<div class="manut-actions">' +
                '<button class="btn btn-danger" onclick="deletarFilial(' + f.id + ')">Excluir</button>' +
            '</div>' +
        '</div>';
    }).join('');
}

function criarFilial() {
    var body = {
        nome: document.getElementById('fil-nome').value.trim(),
        codigo: document.getElementById('fil-codigo').value.trim(),
        cidade: document.getElementById('fil-cidade').value.trim(),
        estado: document.getElementById('fil-estado').value.trim()
    };
    if (!body.nome) return;
    fetch('/filiais', { method:'POST', headers: authHeaders({'Content-Type':'application/json'}), body: JSON.stringify(body) })
        .then(function(r){ return r.json().then(function(d){ return {s:r.status, d:d}; }); })
        .then(function(res){
            if (res.s >= 200 && res.s < 300) {
                toast('Filial criada','ok');
                ['fil-nome','fil-codigo','fil-cidade','fil-estado'].forEach(function(i){ document.getElementById(i).value=''; });
                carregarFiliais();
            } else { toast(res.d.message || 'Erro','err'); }
        });
}

function deletarFilial(id) {
    if (!confirm('Excluir filial?')) return;
    fetch('/filiais/' + id, { method:'DELETE', headers: authHeaders() })
        .then(function(r){ return r.json().then(function(d){ return {s:r.status, d:d}; }); })
        .then(function(res){
            if (res.s >= 200 && res.s < 300) { toast('Excluida','ok'); carregarFiliais(); }
            else { toast(res.d.message || 'Erro','err'); }
        });
}

function abrirModalSetores() { window.location.href = '/setores'; }

// ===== EXPORTAR EXCEL =====
function exportarExcel() {
    var setor = document.getElementById('f-setor').value;
    var filial = document.getElementById('f-filial').value;
    var params = new URLSearchParams();
    if (setor) params.append('setor', setor);
    if (filial) params.append('filial', filial);
    // precisa enviar token via header - usamos fetch e convertemos em blob
    fetch('/agentes/export/excel?' + params.toString(), { headers: authHeaders() })
        .then(function(r){
            if (!r.ok) { toast('Erro ao exportar. Verifique login.', 'err'); throw new Error(); }
            return r.blob();
        })
        .then(function(blob){
            var url = URL.createObjectURL(blob);
            var a = document.createElement('a');
            a.href = url;
            a.download = 'equipamentos.xlsx';
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
            toast('Excel baixado', 'ok');
        })
        .catch(function(){});
}

// ===== WEBSOCKET PARA ATUALIZAR LISTA =====
function inicializarWS() {
    try {
        var s = new SockJS('/ws');
        var stomp = Stomp.over(s);
        stomp.debug = null;
        stomp.connect({}, function(){
            stomp.subscribe('/topic/agentes', function(){
                // Atualiza silenciosamente
                carregar();
            });
        });
    } catch(e) {}
}

// ===== INIT =====
window.addEventListener('DOMContentLoaded', function(){
    if (!AUTH_TOKEN) {
        toast('Faca login para acessar todos os recursos', 'err');
    }
    carregarFiliais().then(function(){ carregar(); });
    setInterval(carregar, 15000);
});
