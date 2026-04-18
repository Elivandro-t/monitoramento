package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.AuditService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/agentes")
@CrossOrigin(origins = "*")
@Tag(name = "Agentes", description = "Gestao de equipamentos/agentes clientes e metricas em tempo real")
public class AgenteController {

    private final AgenteRepository agenteRepository;
    private final AgenteMetricaRepository metricaRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final AuthService authService;
    private final AuditService auditService;
    private final SimpMessagingTemplate messagingTemplate;
    private final EquipamentoSetoresRepository equipamentoSetoresRepository;

    public AgenteController(AgenteRepository agenteRepository,
                           AgenteMetricaRepository metricaRepository,
                           ManutencaoRepository manutencaoRepository,
                           AuthService authService,
                           AuditService auditService,
                           SimpMessagingTemplate messagingTemplate,
                           EquipamentoSetoresRepository equipamentoSetoresRepository) {
        this.agenteRepository = agenteRepository;
        this.metricaRepository = metricaRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.messagingTemplate = messagingTemplate;
        this.equipamentoSetoresRepository = equipamentoSetoresRepository;
    }

    // ===== ENDPOINT QUE O AGENTE CHAMA PARA ENVIAR METRICAS =====
    // Auto-cria o agente se ele nao existir ainda no banco.
    @Operation(summary = "Heartbeat do agente cliente",
            description = "Recebe metricas do agente (CPU, memoria, disco, AnyDesk, hostname) e auto-cria o agente caso nao exista.")
    @PostMapping("/heartbeat")
    @Transactional
    public ResponseEntity<?> heartbeat(@RequestBody Map<String, Object> body) {
        String agentId = (String) body.get("agentId");
        if (agentId == null || agentId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "agentId obrigatorio"));
        }

        var agente = agenteRepository.findByAgentId(agentId).orElseGet(() -> {
            AgenteEntity novo = new AgenteEntity(
                    agentId,
                    str(body.get("hostname")),
                    str(body.get("setor")),
                    str(body.get("filial")),
                    str(body.get("anydesk")),
                    str(body.get("dominio"))
            );
            novo.setDataCadastro(LocalDateTime.now());
            return novo;
        });

        agente.setAtivo(true);
        agente.setStatus("ONLINE");
        agente.setUltimoContato(LocalDateTime.now());
        if (body.containsKey("cpu")) agente.setCpuPercent(toDouble(body.get("cpu")));
        if (body.containsKey("memoriaPercent")) agente.setMemoriaPercent(toDouble(body.get("memoriaPercent")));
        if (body.containsKey("memoriaTotal")) agente.setMemoriaTotal(toLong(body.get("memoriaTotal")));
        if (body.containsKey("memoriaUsada")) agente.setMemoriaUsada(toLong(body.get("memoriaUsada")));
        if (body.containsKey("discoTotal")) agente.setDiscoTotal(toLong(body.get("discoTotal")));
        if (body.containsKey("discoUsado")) agente.setDiscoUsado(toLong(body.get("discoUsado")));
        if (body.containsKey("hostname")) agente.setHostname(str(body.get("hostname")));
        if (body.containsKey("ip")) agente.setIp(str(body.get("ip")));
        if (body.containsKey("so")) agente.setSistemaOperacional(str(body.get("so")));
        if (body.containsKey("usuario")) agente.setUsuario(str(body.get("usuario")));
        if (body.containsKey("anydesk")) agente.setAnydesk(str(body.get("anydesk")));
        if (body.containsKey("dominio")) agente.setDominio(str(body.get("dominio")));
        if(body.containsKey("setor")) agente.setSetor(str(body.get("setor")));
        if(body.containsKey("patrimonio")) agente.setPatrimonio(str(body.get("patrimonio")));
        if(body.containsKey("filial")) agente.setFilial(str(body.get("filial")));

        agenteRepository.save(agente);

        // Salvar metrica historica
        metricaRepository.save(new AgenteMetricaEntity(
                agentId, agente.getCpuPercent(), agente.getMemoriaPercent(),
                agente.getMemoriaUsada(), "ONLINE"));

        // Notificar via WebSocket
        messagingTemplate.convertAndSend("/topic/agentes", Map.of(
                "tipo", "heartbeat",
                "agentId", agentId,
                "cpu", agente.getCpuPercent() != null ? agente.getCpuPercent() : 0,
                "memoria", agente.getMemoriaPercent() != null ? agente.getMemoriaPercent() : 0,
                "disco", calcDiscoPercent(agente),
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        ));

        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    // ===== LISTAR AGENTES (com filtro por setor/filial) =====
    @Operation(summary = "Lista equipamentos com filtros opcionais por setor e filial")
    @GetMapping
    public ResponseEntity<?> listar(@Parameter(description = "Setor para filtrar") @RequestParam(required = false) String setor,
                                    @Parameter(description = "Filial para filtrar") @RequestParam(required = false) String filial,
                                    @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        List<AgenteEntity> agentes;
        if (setor != null && !setor.isBlank() && filial != null && !filial.isBlank()) {
            agentes = agenteRepository.findBySetorAndFilial(setor, filial);
        } else if (setor != null && !setor.isBlank()) {
            agentes = agenteRepository.findBySetor(setor);
        } else if (filial != null && !filial.isBlank()) {
            agentes = agenteRepository.findByFilial(filial);
        } else {
            agentes = agenteRepository.findAll();
        }
        var lista = agentes.stream().map(this::toMap).toList();
        return ResponseEntity.ok(lista);
    }

    // ===== CADASTRAR AGENTE =====
    @Operation(summary = "Cadastra um novo equipamento manualmente")
    @PostMapping
    @Transactional
    public ResponseEntity<?> cadastrar(@RequestBody Map<String, Object> body,
                                       @RequestHeader(value = "Authorization", required = false) String token,
                                       HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        String agentId = str(body.get("agentId"));
        if (agentId == null || agentId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "ID do agente obrigatorio"));
        }
        if (agenteRepository.findByAgentId(agentId).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Agente ja cadastrado"));
        }

        String ipRecebido = str(body.get("ipRecebido"));
        var agente = new AgenteEntity(agentId,
                str(body.get("hostname")),
                str(body.get("setor")),
                str(body.get("filial")),
                str(body.get("anydesk")),
                str(body.get("dominio")));
        agente.setIpRecebido(ipRecebido); // Configurar o IP recebido
        aplicarCamposEquipamento(agente, body);
        agente.recalcularStatusUso();
        agenteRepository.save(agente);

        auditService.registrar(authService.getUsername(token), "CADASTRAR_AGENTE", "/agentes",
                "Cadastrou agente: " + agentId, getClientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("message", "Agente cadastrado com sucesso"));
    }

    // ===== EDITAR AGENTE =====
    @Operation(summary = "Edita um equipamento existente (inclui dados de compra, garantia, descricao, etc.)")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable long id,
                                    @RequestBody Map<String, Object> body,
                                    @RequestHeader(value = "Authorization", required = false) String token,
                                    HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var opt = agenteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        var ag = opt.get();
        if (body.containsKey("hostname")) ag.setHostname(str(body.get("hostname")));
        if (body.containsKey("setor")) ag.setSetor(str(body.get("setor")));
        if (body.containsKey("filial")) ag.setFilial(str(body.get("filial")));
        if (body.containsKey("anydesk")) ag.setAnydesk(str(body.get("anydesk")));
        if (body.containsKey("dominio")) ag.setDominio(str(body.get("dominio")));
        aplicarCamposEquipamento(ag, body);
        ag.recalcularStatusUso();
        agenteRepository.save(ag);

        auditService.registrar(authService.getUsername(token), "EDITAR_AGENTE", "/agentes/" + id,
                "Editou agente: " + ag.getAgentId(), getClientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("message", "Agente atualizado"));
    }

    // ===== DELETAR AGENTE =====
    @Operation(summary = "Remove um equipamento (requer permissao acessoDelete)")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deletar(@PathVariable long id,
                                     @RequestHeader(value = "Authorization", required = false) String token,
                                     HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoDelete")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var opt = agenteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        String agId = opt.get().getAgentId();
        agenteRepository.deleteById(id);

        auditService.registrar(authService.getUsername(token), "DELETAR_AGENTE", "/agentes/" + id,
                "Deletou agente: " + agId, getClientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("message", "Agente excluido"));
    }

    // ===== DETALHE DO AGENTE =====
    @Operation(summary = "Retorna detalhes completos do equipamento incluindo historico de manutencoes, garantia, anos de uso e status de uso.")
    @GetMapping("/{id}")
    public ResponseEntity<?> detalhe(@PathVariable long id,
                                     @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var opt = agenteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toMapDetalhado(opt.get()));
    }

    // ===== METRICAS HISTORICAS =====
    @Operation(summary = "Retorna metricas historicas (CPU, memoria) do equipamento")
    @GetMapping("/{id}/metricas")
    public ResponseEntity<?> metricas(@PathVariable long id,
                                      @RequestParam(defaultValue = "1") int hours,
                                      @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var opt = agenteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        String agentId = opt.get().getAgentId();
        List<AgenteMetricaEntity> metricas;
        if (hours > 0) {
            metricas = metricaRepository.findByAgentIdAndTimestampAfterOrderByTimestampDesc(
                    agentId, LocalDateTime.now().minusHours(hours));
        } else {
            metricas = metricaRepository.findTop500ByAgentIdOrderByTimestampDesc(agentId);
        }

        var lista = metricas.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("cpu", m.getCpuPercent());
            map.put("memoria", m.getMemoriaPercent());
            map.put("memoriaUsada", m.getMemoriaUsada());
            map.put("status", m.getStatus());
            map.put("timestamp", m.getTimestamp() != null ?
                    m.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
            return map;
        }).toList();

        return ResponseEntity.ok(lista);
    }

    // ===== EXCEL EXPORT (todos os equipamentos) =====
    @Operation(summary = "Exporta lista de equipamentos em Excel (xlsx) com cabecalho, filtros e freeze pane")
    @GetMapping("/export/excel")
    @Transactional
    public void exportarExcel(@Parameter(description = "Filtro opcional por setor") @RequestParam(required = false) String setor,
                              @Parameter(description = "Filtro opcional por filial") @RequestParam(required = false) String filial,
                              @RequestHeader(value = "Authorization", required = false) String token,
                              HttpServletResponse response) throws IOException {
        if (token == null || token.isBlank()) {
            // permite via query para download direto, caso a UI use link com token
        }
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            response.sendError(403, "Sem permissao");
            return;
        }

        List<AgenteEntity> agentes;
        if (setor != null && !setor.isBlank() && filial != null && !filial.isBlank()) {
            agentes = agenteRepository.findBySetorAndFilial(setor, filial);
        } else if (setor != null && !setor.isBlank()) {
            agentes = agenteRepository.findBySetor(setor);
        } else if (filial != null && !filial.isBlank()) {
            agentes = agenteRepository.findByFilial(filial);
        } else {
            agentes = agenteRepository.findAll();
        }

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Equipamentos");

        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        String[] headers = {
                "ID", "AgentId", "Hostname", "IP", "Tipo", "Marca", "Modelo", "Patrimonio",
                "NumeroSerie", "Setor", "Filial", "AnyDesk", "Dominio", "SO", "Usuario",
                "Data Cadastro", "Data Compra", "Anos Uso", "Garantia Expiracao", "Garantia Vencida",
                "Manutencoes", "Status Uso", "Status", "CPU %", "Memoria %", "Disco %",
                "Ultimo Contato", "Descricao"
        };

        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (AgenteEntity a : agentes) {
            Row row = sheet.createRow(rowIdx++);
            int col = 0;
            row.createCell(col++).setCellValue(a.getId());
            row.createCell(col++).setCellValue(nz(a.getAgentId()));
            row.createCell(col++).setCellValue(nz(a.getHostname()));
            row.createCell(col++).setCellValue(nz(a.getIp()));
            row.createCell(col++).setCellValue(nz(a.getTipo()));
            row.createCell(col++).setCellValue(nz(a.getMarca()));
            row.createCell(col++).setCellValue(nz(a.getModelo()));
            row.createCell(col++).setCellValue(nz(a.getPatrimonio()));
            row.createCell(col++).setCellValue(nz(a.getNumeroSerie()));
            row.createCell(col++).setCellValue(nz(a.getSetor()));
            row.createCell(col++).setCellValue(nz(a.getFilial()));
            row.createCell(col++).setCellValue(nz(a.getAnydesk()));
            row.createCell(col++).setCellValue(nz(a.getDominio()));
            row.createCell(col++).setCellValue(nz(a.getSistemaOperacional()));
            row.createCell(col++).setCellValue(nz(a.getUsuario()));
            row.createCell(col++).setCellValue(a.getDataCadastro() != null ?
                    a.getDataCadastro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
            row.createCell(col++).setCellValue(a.getDataCompra() != null ?
                    a.getDataCompra().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
            row.createCell(col++).setCellValue(a.anosDesdeCompra() != null ? a.anosDesdeCompra() : 0);
            row.createCell(col++).setCellValue(a.getGarantiaExpiracao() != null ?
                    a.getGarantiaExpiracao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
            row.createCell(col++).setCellValue(a.garantiaVencida() ? "SIM" : "NAO");
            row.createCell(col++).setCellValue(a.getNumManutencoes());
            row.createCell(col++).setCellValue(nz(a.getStatusUso()));
            row.createCell(col++).setCellValue(nz(a.getStatus()));
            row.createCell(col++).setCellValue(a.getCpuPercent() != null ? a.getCpuPercent() : 0);
            row.createCell(col++).setCellValue(a.getMemoriaPercent() != null ? a.getMemoriaPercent() : 0);
            row.createCell(col++).setCellValue(calcDiscoPercent(a));
            row.createCell(col++).setCellValue(a.getUltimoContato() != null ?
                    a.getUltimoContato().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
            row.createCell(col++).setCellValue(nz(a.getDescricao()));
        }

        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
        // Habilita autofilter para toda a area de dados
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                0, Math.max(0, rowIdx - 1), 0, headers.length - 1));
        sheet.createFreezePane(0, 1);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fname = "equipamentos_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fname + "\"");
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
            wb.close();
        }
    }

    // ===== JOB: marcar agentes offline se sem contato ha 2 min =====
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void verificarAgentesOffline() {
        var todos = agenteRepository.findByAtivoTrue();
        var limite = LocalDateTime.now().minusMinutes(2);
        for (var ag : todos) {
            if (ag.getUltimoContato() != null && ag.getUltimoContato().isBefore(limite)) {
                ag.setAtivo(false);
                ag.setStatus("OFFLINE");
                agenteRepository.save(ag);
                metricaRepository.save(new AgenteMetricaEntity(ag.getAgentId(), 0.0, 0.0, 0L, "OFFLINE"));
            }
        }
    }

    // ===== JOB: limpar metricas antigas (2 dias) =====
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void limparMetricasAntigas() {
        var limite = LocalDateTime.now().minusDays(2);
        var antigas = metricaRepository.findAllByTimestampBefore(limite);
        if (!antigas.isEmpty()) metricaRepository.deleteAll(antigas);
    }

    // ===== Map helpers =====
    private Map<String, Object> toMap(AgenteEntity a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("agentId", a.getAgentId());
        m.put("hostname", nz(a.getHostname()));
        m.put("ip", nz(a.getIp()));
        m.put("setor", nz(a.getSetor()));
        m.put("filial", nz(a.getFilial()));
        m.put("anydesk", nz(a.getAnydesk()));
        m.put("dominio", nz(a.getDominio()));
        m.put("so", nz(a.getSistemaOperacional()));
        m.put("usuario", nz(a.getUsuario()));
        m.put("tipo", nz(a.getTipo()));
        m.put("marca", nz(a.getMarca()));
        m.put("modelo", nz(a.getModelo()));
        m.put("patrimonio", nz(a.getPatrimonio()));
        m.put("numeroSerie", nz(a.getNumeroSerie()));
        m.put("ativo", a.isAtivo());
        m.put("status", nz(a.getStatus()));
        m.put("cpu", a.getCpuPercent() != null ? a.getCpuPercent() : 0);
        m.put("memoriaPercent", a.getMemoriaPercent() != null ? a.getMemoriaPercent() : 0);
        m.put("memoriaTotal", a.getMemoriaTotal() != null ? a.getMemoriaTotal() : 0);
        m.put("memoriaUsada", a.getMemoriaUsada() != null ? a.getMemoriaUsada() : 0);
        m.put("discoTotal", a.getDiscoTotal() != null ? a.getDiscoTotal() : 0);
        m.put("discoUsado", a.getDiscoUsado() != null ? a.getDiscoUsado() : 0);
        m.put("discoPercent", calcDiscoPercent(a));
        m.put("statusUso", nz(a.getStatusUso()));
        m.put("numManutencoes", a.getNumManutencoes());
        m.put("dataCompra", a.getDataCompra() != null ? a.getDataCompra().toString() : "");
        m.put("garantiaExpiracao", a.getGarantiaExpiracao() != null ? a.getGarantiaExpiracao().toString() : "");
        m.put("anosUso", a.anosDesdeCompra() != null ? a.anosDesdeCompra() : 0);
        m.put("garantiaVencida", a.garantiaVencida());
        m.put("imagemUrl", nz(a.getImagemUrl()));
        m.put("descricao", nz(a.getDescricao()));
        m.put("ultimoContato", a.getUltimoContato() != null ?
                a.getUltimoContato().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
        m.put("dataCadastro", a.getDataCadastro() != null ?
                a.getDataCadastro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
        return m;
    }

    private Map<String, Object> toMapDetalhado(AgenteEntity a) {
        Map<String, Object> m = toMap(a);
        var manuts = manutencaoRepository.findByAgentIdOrderByDataManutencaoDesc(a.getAgentId());
        var listaManut = manuts.stream().map(x -> {
            Map<String, Object> r = new HashMap<>();
            r.put("id", x.getId());
            r.put("tecnico", nz(x.getTecnicoNome()) + " (" + nz(x.getTecnicoUsername()) + ")");
            r.put("tecnicoUsername", nz(x.getTecnicoUsername()));
            r.put("data", x.getDataManutencao() != null ?
                    x.getDataManutencao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
            r.put("descricao", nz(x.getDescricao()));
            r.put("laudo", nz(x.getLaudoFinal()));
            r.put("anexo", nz(x.getAnexoOriginal()));
            r.put("imagem", nz(x.getImagem()));
            return r;
        }).toList();
        m.put("manutencoes", listaManut);
        return m;
    }

    private void aplicarCamposEquipamento(AgenteEntity ag, Map<String, Object> body) {
        if (body.containsKey("tipo")) ag.setTipo(str(body.get("tipo")));
        if (body.containsKey("marca")) ag.setMarca(str(body.get("marca")));
        if (body.containsKey("modelo")) ag.setModelo(str(body.get("modelo")));
        if (body.containsKey("patrimonio")) ag.setPatrimonio(str(body.get("patrimonio")));
        if (body.containsKey("numeroSerie")) ag.setNumeroSerie(str(body.get("numeroSerie")));
        if (body.containsKey("descricao")) ag.setDescricao(str(body.get("descricao")));
        if (body.containsKey("imagemUrl")) ag.setImagemUrl(str(body.get("imagemUrl")));
        if (body.containsKey("dataCompra")) {
            String dc = str(body.get("dataCompra"));
            ag.setDataCompra(dc == null || dc.isBlank() ? null : LocalDate.parse(dc));
        }
        if (body.containsKey("garantiaExpiracao")) {
            String dg = str(body.get("garantiaExpiracao"));
            ag.setGarantiaExpiracao(dg == null || dg.isBlank() ? null : LocalDate.parse(dg));
        }
    }

    private double calcDiscoPercent(AgenteEntity a) {
        if (a.getDiscoTotal() == null || a.getDiscoTotal() <= 0 || a.getDiscoUsado() == null) return 0;
        return ((double) a.getDiscoUsado() / a.getDiscoTotal()) * 100.0;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private Double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isEmpty()) ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }

    @Operation(summary = "New Route Example", description = "This is a new route secured with user permissions.")
    @GetMapping("/new-route")
    public ResponseEntity<String> newRoute(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (authService.temPermissao(token, "requiredPermission")) {
            return ResponseEntity.ok("Access granted to new route.");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
    }

    // Adicionar lógica para salvar equipamentos na entidade EquipamentoSetores
    @PostMapping("/equipamentos")
    @Transactional
    public ResponseEntity<?> salvarEquipamento(@RequestBody Map<String, Object> body) {
        EquipamentoSetores equipamento = new EquipamentoSetores();
        equipamento.setSetor((String) body.get("setor"));
        equipamento.setEquipamento((String) body.get("equipamento"));
        equipamento.setDescricao((String) body.get("descricao"));

        // Salvar na entidade EquipamentoSetores
        equipamentoSetoresRepository.save(equipamento);

        return ResponseEntity.ok(Map.of("message", "Equipamento salvo com sucesso"));
    }
}