var AUTH_TOKEN = localStorage.getItem('auth_token') || '';
var todosEquipamentos = [];
var filiaisCache = [];
var mtsetoresCache = [];
var agenteSelecionado = null;
var usuarioLogado = null; // { username, nome, perfil }
// Guarda qual campo esta sendo desbloqueado no modal de autenticacao
var desbloqueioAlvo = null; // { inputId, badgeId }

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

// ===== LOCK / UNLOCK POR CAMPO =====
function travarCampo(inputId, badgeId) {
    var inp = document.getElementById(inputId);
    if (inp) { inp.readOnly = true; }
    var badge = document.getElementById(badgeId);
    if (badge) { badge.innerHTML = '&#128274;'; badge.title = 'Travado'; badge.classList.remove('unlocked'); }
    // Garante que o botao Desbloquear volte a aparecer se existir
    var wrap = inp ? inp.parentElement : null;
    if (wrap) {
        var btn = wrap.querySelector('.btn-unlock');
        if (btn) btn.style.display = '';
    }
}

function destravarCampo(inputId, badgeId) {
    var inp = document.getElementById(inputId);
    if (inp) { inp.readOnly = false; inp.focus(); }
    var badge = document.getElementById(badgeId);
    if (badge) { badge.innerHTML = '&#128275;'; badge.title = 'Liberado'; badge.classList.add('unlocked'); }
    var wrap = inp ? inp.parentElement : null;
    if (wrap) {
        var btn = wrap.querySelector('.btn-unlock');
        if (btn) btn.style.display = 'none';
    }
}

function pedirDesbloqueio(inputId, badgeId) {
    desbloqueioAlvo = { inputId: inputId, badgeId: badgeId };
    document.getElementById('auth-user').value = '';
    document.getElementById('auth-pass').value = '';
    document.getElementById('auth-err').style.display = 'none';
    abrirModal('modal-auth-field');
    setTimeout(function(){ document.getElementById('auth-user').focus(); }, 50);
}

function confirmarDesbloqueio() {
    if (!desbloqueioAlvo) return;
    var user = document.getElementById('auth-user').value.trim();
    var pass = document.getElementById('auth-pass').value;
    if (!user || !pass) {
        document.getElementById('auth-err').innerText = 'Preencha usuario e senha';
        document.getElementById('auth-err').style.display = 'block';
        return;
    }
    fetch('/auth/revalidar', {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ username: user, senha: pass })
    })
    .then(function(r){ return r.json().then(function(d){ return {s:r.status, d:d}; }); })
    .then(function(res){
        if (res.s >= 200 && res.s < 300 && res.d.autorizado) {
            destravarCampo(desbloqueioAlvo.inputId, desbloqueioAlvo.badgeId);
            fecharModal('modal-auth-field');
            desbloqueioAlvo = null;
            toast('Campo liberado para edicao', 'ok');
        } else {
            document.getElementById('auth-err').innerText = res.d.message || 'Credenciais invalidas';
            document.getElementById('auth-err').style.display = 'block';
        }
    })
    .catch(function(){
        document.getElementById('auth-err').innerText = 'Erro de conexao';
        document.getElementById('auth-err').style.display = 'block';
    });
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
                // Filtro continua por nome (para casar com o que esta gravado)
                var o1 = document.createElement('option'); o1.value = f.nome; o1.textContent = f.nome;
                selFiltro.appendChild(o1);
                // Editor envia o NUMERO (codigo) da filial, mas mostra "codigo - nome"
                var codigo = (f.codigo != null && String(f.codigo).trim() !== '') ? String(f.codigo) : String(f.id);
                var o2 = document.createElement('option');
                o2.value = codigo;
                o2.textContent = codigo + (f.nome ? ' - ' + f.nome : '');
                selEq.appendChild(o2);
            });
            atualizarListaFiliais();
        })
        .catch(function(){});
}

function carregarMTsetores() {
    return fetch('/mtsetores', { headers: authHeaders() })
        .then(function(r){ return r.json(); })
        .then(function(data){
            mtsetoresCache = Array.isArray(data) ? data : [];
            var selFiltro = document.getElementById('f-mtsetor');
            var selEq = document.getElementById('eq-mtsetor');
            var selMtsFil = document.getElementById('mts-filial');
            if (selFiltro) selFiltro.innerHTML = '<option value="">Todos</option>';
            if (selEq) selEq.innerHTML = '<option value="">--</option>';
            if (selMtsFil) {
                selMtsFil.innerHTML = '<option value="">--</option>';
                filiaisCache.forEach(function(f){
                    var o = document.createElement('option'); o.value = f.nome; o.textContent = f.nome;
                    selMtsFil.appendChild(o);
                });
            }
            mtsetoresCache.forEach(function(s){
                if (selFiltro) {
                    var o1 = document.createElement('option'); o1.value = s.nome; o1.textContent = s.nome;
                    selFiltro.appendChild(o1);
                }
                if (selEq) {
                    var o2 = document.createElement('option'); o2.value = s.nome; o2.textContent = s.nome;
                    selEq.appendChild(o2);
                }
            });
            atualizarListaMTsetores();
        })
        .catch(function(){});
}

// Converte um valor de filial (codigo OU nome) no codigo correspondente,
// para o select do editor, que opera por numero/codigo.
function resolverCodigoFilial(valor) {
    if (!valor) return '';
    var s = String(valor).trim();
    for (var i = 0; i < filiaisCache.length; i++) {
        var f = filiaisCache[i];
        var cod = (f.codigo != null && String(f.codigo).trim() !== '') ? String(f.codigo) : String(f.id);
        if (cod === s) return cod;
        if (f.nome && f.nome === s) return cod;
    }
    return s; // desconhecido: mantem o valor, sera adicionado via garantirOpcao
}

// Garante que o select contenha a opcao <value>, adicionando-a se ausente.
function garantirOpcao(selectId, valor) {
    if (!valor) return;
    var sel = document.getElementById(selectId);
    if (!sel) return;
    for (var i = 0; i < sel.options.length; i++) {
        if (sel.options[i].value === valor) return;
    }
    var o = document.createElement('option');
    o.value = valor; o.textContent = valor;
    sel.appendChild(o);
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
    var setor = document.getElementById('f-mtsetor').value;
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
    ['f-busca','f-mtsetor','f-filial','f-status','f-uso','f-tipo'].forEach(function(id){
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
            '<td><span class="uso-chip uso-' + (a.statusUso || 'NORMAL') + '" title="' + esc(a.usoTooltip || 'Uso normal') + '">' + (a.statusUso || 'NORMAL') + '</span></td>' +
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

// Converte bytes em formato legivel (GB ou MB)
function bytesParaGB(bytes) {
    var n = Number(bytes);
    if (!n || isNaN(n) || n <= 0) return null;
    var gb = n / 1073741824;
    if (gb >= 1) return gb.toFixed(2) + ' GB';
    var mb = n / 1048576;
    return mb.toFixed(0) + ' MB';
}

// "2.34 GB / 7.89 GB (29.7%)"
function formatarUso(usada, total, percent) {
    var pct = (percent || 0).toFixed(1) + '%';
    var t = bytesParaGB(total);
    var u = bytesParaGB(usada);
    if (!t && !u) return pct;
    if (!t) return (u || '--') + ' (' + pct + ')';
    if (!u) return '-- / ' + t + ' (' + pct + ')';
    return u + ' / ' + t + ' (' + pct + ')';
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
    document.getElementById('eq-mtsetor').value = '';
    document.getElementById('eq-filial').value = '';
    // Em cadastro novo, agentId/hostname/anydesk ficam livres para digitar.
    destravarCampo('eq-agentId','lock-agentId');
    destravarCampo('eq-hostname','lock-hostname');
    destravarCampo('eq-anydesk','lock-anydesk');
    abrirModal('modal-equip');
}

function editarEquip(id) {
    var a = todosEquipamentos.find(function(x){ return x.id === id; });
    if (!a) return;
    document.getElementById('modal-equip-titulo').innerText = 'Editar: ' + (a.hostname || a.agentId);
    document.getElementById('eq-id').value = a.id;
    document.getElementById('eq-agentId').value = a.agentId || '';
    document.getElementById('eq-hostname').value = a.hostname || '';
    // Ao editar, os 3 campos criticos comecam travados; so liberam com senha.
    travarCampo('eq-agentId','lock-agentId');
    travarCampo('eq-hostname','lock-hostname');
    travarCampo('eq-anydesk','lock-anydesk');
    document.getElementById('eq-patrimonio').value = a.patrimonio || '';
    document.getElementById('eq-marca').value = a.marca || '';
    document.getElementById('eq-modelo').value = a.modelo || '';
    document.getElementById('eq-sn').value = a.numeroSerie || '';
    document.getElementById('eq-anydesk').value = a.anydesk || '';
    document.getElementById('eq-dominio').value = a.dominio || '';
    document.getElementById('eq-tipo').value = a.tipo || '';
    // Garante que o setor vindo do agente apareca mesmo sem cadastro previo
    garantirOpcao('eq-mtsetor', a.setor || '');
    document.getElementById('eq-mtsetor').value = a.setor || '';
    // Filial no editor usa o NUMERO (codigo). Se o valor salvo for o nome da filial,
    // convertemos para o codigo correspondente (sem alterar o registro).
    var filialValor = resolverCodigoFilial(a.filial || '');
    garantirOpcao('eq-filial', filialValor);
    document.getElementById('eq-filial').value = filialValor;
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
        setor: document.getElementById('eq-mtsetor').value,
        filial: document.getElementById('eq-filial').value,
        dataCompra: document.getElementById('eq-dataCompra').value,
        garantiaExpiracao: document.getElementById('eq-garantia').value,
        descricao: document.getElementById('eq-descricao').value.trim()
    };
    // Cadastro manual: envia agentId vazio (backend gera MAN-xxxx).
    if (!body.agentId && id) { toast('Agent ID obrigatorio ao editar', 'err'); return; }
    if (!body.agentId) { delete body.agentId; }

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

function renderBadgeManutencao(a) {
    var el = document.getElementById('det-manut-badge');
    if (!el) return;
    var s = a.statusManutencao || 'EM_DIA';
    var msg = a.statusManutencaoMsg || 'Manutencao em dia';
    var map = {
        'CRITICO':   'manut-badge manut-badge-critico',
        'ATENCAO':   'manut-badge manut-badge-atencao',
        'SEM_MANUT': 'manut-badge manut-badge-semmanut',
        'EM_DIA':    'manut-badge manut-badge-emdia'
    };
    el.className = map[s] || map['EM_DIA'];
    el.innerHTML = '<span>' + esc(msg) + '</span>';
    el.style.display = 'flex';
}

function renderDetInfo(a) {
    renderBadgeManutencao(a);
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
        ['Status de Uso', (a.statusUso || 'NORMAL') + (a.usoTooltip ? ' (' + a.usoTooltip + ')' : ''), statusUsoClass],
        ['Status', a.status || '--', a.status === 'ONLINE' ? 'ok' : 'bad'],
        ['CPU', (a.cpu || 0).toFixed(1) + '%'],
        ['Memoria', formatarUso(a.memoriaUsada, a.memoriaTotal, a.memoriaPercent)],
        ['Disco', formatarUso(a.discoUsado, a.discoTotal, a.discoPercent)],
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
    var tecInp = document.getElementById('mn-tecnico');
    if (tecInp) {
        if (usuarioLogado) {
            tecInp.value = (usuarioLogado.nome || usuarioLogado.username || '') +
                ' (' + (usuarioLogado.username || '') + ')';
        } else {
            tecInp.value = '(sem login - faca login para registrar)';
        }
    }
    abrirModal('modal-manut');
}

function carregarUsuarioLogado() {
    if (!AUTH_TOKEN) return Promise.resolve(null);
    return fetch('/auth/verificar', {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ token: AUTH_TOKEN })
    })
    .then(function(r){ return r.json(); })
    .then(function(d){
        if (d && d.autorizado) {
            usuarioLogado = { username: d.username, nome: d.nome || d.username, perfil: d.perfil };
        }
        return usuarioLogado;
    })
    .catch(function(){ return null; });
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

// ===== CONDENAR EQUIPAMENTO =====
function abrirModalCondenar() {
    if (!agenteSelecionado) return;
    document.getElementById('cond-motivo').value = '';
    document.getElementById('cond-laudo').value = '';
    abrirModal('modal-condenar');
}

function confirmarCondenacao() {
    if (!agenteSelecionado) return;
    var motivo = document.getElementById('cond-motivo').value.trim();
    if (!motivo) { toast('Informe o motivo da condenacao', 'err'); return; }
    if (!confirm('Tem certeza que deseja condenar este equipamento? Essa acao remove da lista ativa.')) return;
    var body = {
        motivo: motivo,
        laudoFinal: document.getElementById('cond-laudo').value.trim()
    };
    fetch('/equipamentos-condenados/condenar/' + agenteSelecionado.id, {
        method: 'POST',
        headers: authHeaders({'Content-Type':'application/json'}),
        body: JSON.stringify(body)
    })
    .then(function(r){ return r.json().then(function(d){ return {s:r.status, d:d}; }); })
    .then(function(res){
        if (res.s >= 200 && res.s < 300) {
            toast('Equipamento condenado', 'ok');
            fecharModal('modal-condenar');
            fecharModal('modal-detalhe');
            agenteSelecionado = null;
            carregar();
        } else {
            toast(res.d.message || 'Erro ao condenar', 'err');
        }
    })
    .catch(function(){ toast('Erro de conexao', 'err'); });
}

// ===== HISTORICO DE CONDENADOS =====
function abrirModalCondenados() {
    carregarCondenados();
    abrirModal('modal-condenados');
}

function carregarCondenados() {
    var box = document.getElementById('lista-condenados');
    box.innerHTML = '<div class="loading-wrap"><div class="loader"></div></div>';
    fetch('/equipamentos-condenados', { headers: authHeaders() })
        .then(function(r){ return r.json(); })
        .then(function(data){
            var lista = Array.isArray(data) ? data : [];
            if (!lista.length) {
                box.innerHTML = '<div class="empty-state">Nenhum equipamento condenado</div>';
                return;
            }
            box.innerHTML = lista.map(function(c){
                return '<div class="cond-item">' +
                    '<div class="cond-titulo">' + esc(c.hostname || c.agentId) +
                        (c.patrimonio ? ' <span class="cell-muted">(' + esc(c.patrimonio) + ')</span>' : '') +
                    '</div>' +
                    '<div class="cond-meta">' +
                        'Condenado em ' + esc(c.dataCondenacao) +
                        ' por ' + esc(c.condenadoPor || '--') +
                        ' | ' + esc(c.marca || '--') + ' ' + esc(c.modelo || '') +
                        ' | Setor: ' + esc(c.setor || '--') +
                        ' | Manutencoes: ' + (c.totalManutencoes || 0) +
                    '</div>' +
                    '<div class="cond-motivo"><strong>Motivo:</strong> ' + esc(c.motivoCondenacao || '--') + '</div>' +
                    '<div class="cond-acoes">' +
                        '<button class="btn" onclick="verCondenado(' + c.id + ')">Visualizar</button>' +
                        '<button class="btn btn-danger" onclick="deletarCondenado(' + c.id + ')">Remover do historico</button>' +
                    '</div>' +
                '</div>';
            }).join('');
        })
        .catch(function(){ box.innerHTML = '<div class="empty-state">Erro ao carregar</div>'; });
}

function verCondenado(id) {
    fetch('/equipamentos-condenados/' + id, { headers: authHeaders() })
        .then(function(r){ return r.json(); })
        .then(function(c){
            document.getElementById('cond-det-titulo').innerText =
                'Equipamento Condenado: ' + (c.hostname || c.agentId);
            var items = [
                ['Agent ID', c.agentId],
                ['Hostname', c.hostname],
                ['Tipo', c.tipo || '--'],
                ['Marca', c.marca || '--'],
                ['Modelo', c.modelo || '--'],
                ['Patrimonio', c.patrimonio || '--'],
                ['Numero Serie', c.numeroSerie || '--'],
                ['Setor', c.setor || '--'],
                ['Filial', c.filial || '--'],
                ['Total Manutencoes', c.totalManutencoes || 0],
                ['Condenado por', c.condenadoPor || '--'],
                ['Data Condenacao', c.dataCondenacao || '--'],
                ['Motivo', c.motivoCondenacao || '--']
            ];
            document.getElementById('cond-det-info').innerHTML = items.map(function(it){
                return '<div class="info-item"><span class="info-label">' + esc(it[0]) + '</span>' +
                       '<span class="info-value">' + esc(it[1]) + '</span></div>';
            }).join('');
            document.getElementById('cond-det-laudo').innerText = c.laudoFinal || '(sem laudo final)';
            var hist = Array.isArray(c.historicoManutencoes) ? c.historicoManutencoes : [];
            var histBox = document.getElementById('cond-det-hist');
            if (!hist.length) {
                histBox.innerHTML = '<div class="empty-state">Sem manutencoes registradas</div>';
            } else {
                histBox.innerHTML = hist.map(function(m){
                    return '<div class="manut-item">' +
                        '<div class="manut-header">' +
                            '<div class="manut-tecnico">' + esc(m.tecnico || '--') + '</div>' +
                            '<div class="manut-data">' + esc(m.data || '') + '</div>' +
                        '</div>' +
                        '<div class="manut-desc"><strong>Descricao:</strong> ' + esc(m.descricao || '--') + '</div>' +
                        (m.laudo ? '<div class="manut-laudo">' + esc(m.laudo) + '</div>' : '') +
                    '</div>';
                }).join('');
            }
            abrirModal('modal-condenado-det');
        })
        .catch(function(){ toast('Erro ao carregar detalhes', 'err'); });
}

function deletarCondenado(id) {
    if (!confirm('Remover PERMANENTEMENTE este registro do historico?')) return;
    fetch('/equipamentos-condenados/' + id, { method: 'DELETE', headers: authHeaders() })
        .then(function(r){ return r.json().then(function(d){ return {s:r.status, d:d}; }); })
        .then(function(res){
            if (res.s >= 200 && res.s < 300) {
                toast('Registro removido', 'ok');
                carregarCondenados();
            } else {
                toast(res.d.message || 'Erro', 'err');
            }
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

// ===== MTsetores (Gerenciar Setores) =====
function abrirModalSetores() {
    limparFormMTsetor();
    atualizarListaMTsetores();
    abrirModal('modal-mtsetor');
}

function limparFormMTsetor() {
    document.getElementById('mts-id').value = '';
    document.getElementById('mts-nome').value = '';
    document.getElementById('mts-descricao').value = '';
    var selMtsFil = document.getElementById('mts-filial');
    if (selMtsFil) selMtsFil.value = '';
}

function atualizarListaMTsetores() {
    var box = document.getElementById('lista-mtsetores');
    if (!box) return;
    if (!mtsetoresCache.length) {
        box.innerHTML = '<div class="empty-state">Nenhum setor cadastrado</div>';
        return;
    }
    box.innerHTML = mtsetoresCache.map(function(s){
        return '<div class="manut-item">' +
            '<div class="manut-header">' +
                '<div class="manut-tecnico">' + esc(s.nome) +
                    (s.filial ? ' <span class="cell-muted">(' + esc(s.filial) + ')</span>' : '') +
                '</div>' +
                '<div class="cell-muted">' + esc(s.descricao || '') + '</div>' +
            '</div>' +
            '<div class="manut-actions">' +
                '<button class="btn" onclick="editarMTsetor(' + s.id + ')">Editar</button>' +
                '<button class="btn btn-danger" onclick="deletarMTsetor(' + s.id + ')">Excluir</button>' +
            '</div>' +
        '</div>';
    }).join('');
}

function editarMTsetor(id) {
    var s = mtsetoresCache.find(function(x){ return x.id === id; });
    if (!s) return;
    document.getElementById('mts-id').value = s.id;
    document.getElementById('mts-nome').value = s.nome || '';
    document.getElementById('mts-descricao').value = s.descricao || '';
    garantirOpcao('mts-filial', s.filial || '');
    document.getElementById('mts-filial').value = s.filial || '';
}

function salvarMTsetor() {
    var id = document.getElementById('mts-id').value;
    var body = {
        nome: document.getElementById('mts-nome').value.trim(),
        filial: document.getElementById('mts-filial').value,
        descricao: document.getElementById('mts-descricao').value.trim()
    };
    if (!body.nome) { toast('Nome do setor e obrigatorio', 'err'); return; }
    var url = id ? '/mtsetores/' + id : '/mtsetores';
    var method = id ? 'PUT' : 'POST';
    fetch(url, {
        method: method,
        headers: authHeaders({'Content-Type':'application/json'}),
        body: JSON.stringify(body)
    })
    .then(function(r){ return r.json().then(function(d){ return {s:r.status, d:d}; }); })
    .then(function(res){
        if (res.s >= 200 && res.s < 300) {
            toast(id ? 'Setor atualizado' : 'Setor criado', 'ok');
            limparFormMTsetor();
            carregarMTsetores();
        } else {
            toast(res.d.message || 'Erro ao salvar setor', 'err');
        }
    })
    .catch(function(){ toast('Erro de conexao', 'err'); });
}

function deletarMTsetor(id) {
    if (!confirm('Excluir este setor?')) return;
    fetch('/mtsetores/' + id, { method: 'DELETE', headers: authHeaders() })
        .then(function(r){ return r.json().then(function(d){ return {s:r.status, d:d}; }); })
        .then(function(res){
            if (res.s >= 200 && res.s < 300) { toast('Setor excluido', 'ok'); carregarMTsetores(); }
            else { toast(res.d.message || 'Erro', 'err'); }
        });
}

// ===== EXPORTAR EXCEL =====
function abrirPdfEquipamentos(forceDownload) {
    fetch('/agentes/relatorio.pdf' + (forceDownload ? '?download=1' : ''), { headers: authHeaders() })
        .then(function(r){
            if (!r.ok) { toast('Erro ao gerar PDF', 'err'); throw new Error(); }
            return r.blob();
        })
        .then(function(blob){
            var url = URL.createObjectURL(blob);
            if (forceDownload) {
                var a = document.createElement('a');
                a.href = url; a.download = 'equipamentos.pdf';
                document.body.appendChild(a); a.click(); a.remove();
            } else {
                window.open(url, '_blank');
            }
            setTimeout(function(){ URL.revokeObjectURL(url); }, 30000);
        }).catch(function(){});
}
function visualizarPdfEquipamentos() { abrirPdfEquipamentos(false); }
function baixarPdfEquipamentos() { abrirPdfEquipamentos(true); }

function exportarExcel() {
    var setor = document.getElementById('f-mtsetor').value;
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
    carregarUsuarioLogado();
    carregarFiliais().then(function(){ return carregarMTsetores(); }).then(function(){ carregar(); });
    setInterval(carregar, 15000);
});
