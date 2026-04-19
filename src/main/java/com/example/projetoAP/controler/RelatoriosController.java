package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.controler.service.PdfService;
import com.example.projetoAP.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/relatorios")
@CrossOrigin(origins = "*")
@Tag(name = "Relatorios", description = "Graficos e exportacao de manutencoes, condenados e produtividade")
public class RelatoriosController {

    private final ManutencaoRepository manutencaoRepository;
    private final EquipamentoCondenadoRepository condenadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;
    private final PdfService pdfService;

    public RelatoriosController(ManutencaoRepository manutencaoRepository,
                                EquipamentoCondenadoRepository condenadoRepository,
                                UsuarioRepository usuarioRepository,
                                AuthService authService,
                                PdfService pdfService) {
        this.manutencaoRepository = manutencaoRepository;
        this.condenadoRepository = condenadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
        this.pdfService = pdfService;
    }

    // ===== PDF: Ranking =====
    @Operation(summary = "PDF - Ranking de manutencoes (inline)")
    @GetMapping("/ranking-manutencoes.pdf")
    public ResponseEntity<byte[]> rankingPdf(@RequestParam(defaultValue = "mes") String periodo,
                                             @RequestParam(required = false) String download,
                                             @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).build();
        }
        @SuppressWarnings("unchecked")
        var body = (Map<String, Object>) ranking(periodo, token).getBody();
        if (body == null) return ResponseEntity.internalServerError().build();
        @SuppressWarnings("unchecked")
        var rank = (List<Map<String, Object>>) body.get("ranking");
        byte[] pdf = pdfService.gerarRelatorioRanking(rank, periodo);
        return pdfResponse(pdf, "ranking_manutencoes.pdf", "1".equals(download));
    }

    // ===== PDF: Metricas =====
    @Operation(summary = "PDF - Metricas por tecnico/operador")
    @GetMapping("/metricas.pdf")
    public ResponseEntity<byte[]> metricasPdf(@RequestParam(required = false) String download,
                                              @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).build();
        }
        @SuppressWarnings("unchecked")
        var body = (Map<String, Object>) metricas(token).getBody();
        if (body == null) return ResponseEntity.internalServerError().build();
        @SuppressWarnings("unchecked")
        var usuarios = (List<Map<String, Object>>) body.get("usuarios");
        byte[] pdf = pdfService.gerarRelatorioMetricas(usuarios);
        return pdfResponse(pdf, "metricas_usuarios.pdf", "1".equals(download));
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename, boolean forceDownload) {
        String dispo = (forceDownload ? "attachment" : "inline") + "; filename=\"" + filename + "\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, dispo)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ===== RANKING (/relatorios/ranking-manutencoes) =====
    @Operation(summary = "Ranking de tecnicos por manutencoes realizadas (dia/semana/mes)")
    @GetMapping("/ranking-manutencoes")
    public ResponseEntity<?> ranking(@RequestParam(defaultValue = "mes") String periodo,
                                     @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        LocalDateTime inicio;
        LocalDateTime fim = LocalDate.now().atTime(23, 59, 59);
        switch (periodo.toLowerCase()) {
            case "dia": inicio = LocalDate.now().atStartOfDay(); break;
            case "semana": inicio = LocalDate.now().minusDays(7).atStartOfDay(); break;
            case "mes":
            default: inicio = LocalDate.now().minusDays(30).atStartOfDay();
        }
        Map<String, String> nomes = new HashMap<>();
        Map<String, String> cargos = new HashMap<>();
        usuarioRepository.findAll().forEach(u -> {
            nomes.put(u.getUsername(), u.getNome() != null ? u.getNome() : u.getUsername());
            cargos.put(u.getUsername(), u.getCargo() != null ? u.getCargo() : "");
        });
        var dados = manutencaoRepository.rankingPorPeriodo(inicio, fim);
        List<Map<String, Object>> rank = new ArrayList<>();
        for (Object[] row : dados) {
            String user = (String) row[0];
            Long total = ((Number) row[1]).longValue();
            Map<String, Object> r = new HashMap<>();
            r.put("username", user);
            r.put("nome", nomes.getOrDefault(user, user));
            r.put("cargo", cargos.getOrDefault(user, ""));
            r.put("total", total);
            rank.add(r);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("periodo", periodo.toLowerCase());
        resp.put("inicio", inicio.format(DateTimeFormatter.ISO_DATE));
        resp.put("fim", fim.format(DateTimeFormatter.ISO_DATE));
        resp.put("ranking", rank);
        return ResponseEntity.ok(resp);
    }

    // ===== METRICAS POR USUARIO (/relatorios/metricas/dados) =====
    @Operation(summary = "Metricas por tecnico/operador: total, tempo medio, concluidos")
    @GetMapping("/metricas/dados")
    public ResponseEntity<?> metricas(@RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var usuarios = usuarioRepository.findAll().stream()
                .filter(u -> {
                    String cargo = u.getCargo() == null ? "" : u.getCargo().toUpperCase();
                    return cargo.contains("TECNICO") || cargo.contains("OPERADOR")
                            || "INFORMATICA".equals(u.getPerfil()) || "ADMIN".equals(u.getPerfil());
                })
                .toList();

        Map<String, List<ManutencaoEntity>> porUser = new HashMap<>();
        for (var m : manutencaoRepository.findAll()) {
            if (m.getTecnicoUsername() == null) continue;
            porUser.computeIfAbsent(m.getTecnicoUsername(), k -> new ArrayList<>()).add(m);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (var u : usuarios) {
            var ms = porUser.getOrDefault(u.getUsername(), Collections.emptyList());
            long total = ms.size();
            long concluidos = ms.stream().filter(m -> "ENTREGUE".equals(m.getStatus())
                    || "FINALIZADO".equals(m.getStatus())).count();
            OptionalDouble mediaHoras = ms.stream()
                    .filter(m -> m.getDataEntrada() != null && m.getDataEntrega() != null)
                    .mapToLong(m -> Duration.between(m.getDataEntrada(), m.getDataEntrega()).toHours())
                    .average();
            Map<String, Object> r = new HashMap<>();
            r.put("username", u.getUsername());
            r.put("nome", u.getNome() != null ? u.getNome() : u.getUsername());
            r.put("cargo", u.getCargo() != null ? u.getCargo() : "");
            r.put("perfil", u.getPerfil());
            r.put("total", total);
            r.put("concluidos", concluidos);
            r.put("tempoMedioHoras", mediaHoras.isPresent() ? Math.round(mediaHoras.getAsDouble()) : 0);
            result.add(r);
        }
        result.sort((a, b) -> Long.compare(((Number) b.get("total")).longValue(),
                                          ((Number) a.get("total")).longValue()));
        return ResponseEntity.ok(Map.of("usuarios", result));
    }

    /**
     * Serie temporal de manutencoes agrupadas por dia/mes/ano, com filtro opcional por tecnico.
     * Retorna lista de { label, total }.
     */
    @Operation(summary = "Serie temporal de manutencoes por dia/mes/ano")
    @GetMapping("/manutencoes")
    public ResponseEntity<?> manutencoes(@RequestParam(defaultValue = "dia") String agrupamento,
                                         @RequestParam(required = false) String inicio,
                                         @RequestParam(required = false) String fim,
                                         @RequestParam(required = false) String tecnico,
                                         @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        LocalDateTime ini = parseInicio(inicio, agrupamento);
        LocalDateTime end = parseFim(fim);

        var lista = manutencaoRepository.findAll().stream()
                .filter(m -> m.getDataManutencao() != null)
                .filter(m -> !m.getDataManutencao().isBefore(ini))
                .filter(m -> !m.getDataManutencao().isAfter(end))
                .filter(m -> tecnico == null || tecnico.isBlank()
                        || tecnico.equalsIgnoreCase(m.getTecnicoUsername())
                        || tecnico.equalsIgnoreCase(m.getTecnicoNome()))
                .toList();

        Map<String, Long> agrup = new TreeMap<>();
        DateTimeFormatter fmt = formatterPara(agrupamento);
        for (var m : lista) {
            String chave = m.getDataManutencao().format(fmt);
            agrup.merge(chave, 1L, Long::sum);
        }

        List<Map<String, Object>> data = new ArrayList<>();
        agrup.forEach((k, v) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("label", k);
            row.put("total", v);
            data.add(row);
        });
        Map<String, Object> resp = new HashMap<>();
        resp.put("agrupamento", agrupamento);
        resp.put("total", lista.size());
        resp.put("serie", data);
        return ResponseEntity.ok(resp);
    }

    /**
     * Serie temporal de equipamentos condenados agrupados por dia/mes/ano.
     */
    @Operation(summary = "Serie temporal de equipamentos condenados")
    @GetMapping("/condenados")
    public ResponseEntity<?> condenados(@RequestParam(defaultValue = "mes") String agrupamento,
                                        @RequestParam(required = false) String inicio,
                                        @RequestParam(required = false) String fim,
                                        @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        LocalDateTime ini = parseInicio(inicio, agrupamento);
        LocalDateTime end = parseFim(fim);

        var lista = condenadoRepository.findAll().stream()
                .filter(c -> c.getDataCondenacao() != null)
                .filter(c -> !c.getDataCondenacao().isBefore(ini))
                .filter(c -> !c.getDataCondenacao().isAfter(end))
                .toList();

        Map<String, Long> agrup = new TreeMap<>();
        DateTimeFormatter fmt = formatterPara(agrupamento);
        for (var c : lista) {
            String chave = c.getDataCondenacao().format(fmt);
            agrup.merge(chave, 1L, Long::sum);
        }

        List<Map<String, Object>> data = new ArrayList<>();
        agrup.forEach((k, v) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("label", k);
            row.put("total", v);
            data.add(row);
        });
        Map<String, Object> resp = new HashMap<>();
        resp.put("agrupamento", agrupamento);
        resp.put("total", lista.size());
        resp.put("serie", data);
        return ResponseEntity.ok(resp);
    }

    /**
     * Produtividade por tecnico, agrupada por mes dentro do intervalo.
     * Retorna { tecnicos: [{nome, username, totalManut, porMes: [{mes, total}]}] }.
     */
    @Operation(summary = "Produtividade de cada tecnico por mes")
    @GetMapping("/produtividade")
    public ResponseEntity<?> produtividade(@RequestParam(required = false) String inicio,
                                           @RequestParam(required = false) String fim,
                                           @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        LocalDateTime ini = parseInicio(inicio, "mes");
        LocalDateTime end = parseFim(fim);
        DateTimeFormatter mesFmt = DateTimeFormatter.ofPattern("yyyy-MM");

        var lista = manutencaoRepository.findAll().stream()
                .filter(m -> m.getDataManutencao() != null)
                .filter(m -> !m.getDataManutencao().isBefore(ini))
                .filter(m -> !m.getDataManutencao().isAfter(end))
                .toList();

        // chave = username do tecnico
        Map<String, Map<String, Object>> porTecnico = new TreeMap<>();
        for (var m : lista) {
            String user = m.getTecnicoUsername() != null ? m.getTecnicoUsername() : "desconhecido";
            var box = porTecnico.computeIfAbsent(user, k -> {
                Map<String, Object> b = new HashMap<>();
                b.put("username", user);
                b.put("nome", m.getTecnicoNome() != null ? m.getTecnicoNome() : user);
                b.put("totalManut", 0L);
                b.put("porMes", new TreeMap<String, Long>());
                return b;
            });
            box.put("totalManut", ((Long) box.get("totalManut")) + 1);
            @SuppressWarnings("unchecked")
            Map<String, Long> porMes = (Map<String, Long>) box.get("porMes");
            String mes = m.getDataManutencao().format(mesFmt);
            porMes.merge(mes, 1L, Long::sum);
        }

        List<Map<String, Object>> tecnicos = new ArrayList<>();
        porTecnico.values().forEach(v -> {
            @SuppressWarnings("unchecked")
            Map<String, Long> porMes = (Map<String, Long>) v.get("porMes");
            List<Map<String, Object>> serie = new ArrayList<>();
            porMes.forEach((mes, total) -> {
                Map<String, Object> row = new HashMap<>();
                row.put("mes", mes);
                row.put("total", total);
                serie.add(row);
            });
            v.put("porMes", serie);
            tecnicos.add(v);
        });
        tecnicos.sort((a, b) -> Long.compare(((Long) b.get("totalManut")), ((Long) a.get("totalManut"))));

        Map<String, Object> resp = new HashMap<>();
        resp.put("totalManut", lista.size());
        resp.put("tecnicos", tecnicos);
        return ResponseEntity.ok(resp);
    }

    /**
     * Export Excel combinando os tres relatorios em abas.
     */
    @Operation(summary = "Exporta relatorios (manutencoes, condenados, produtividade) para Excel")
    @GetMapping("/export/excel")
    public void exportExcel(@RequestParam(defaultValue = "dia") String agrupamento,
                            @RequestParam(required = false) String inicio,
                            @RequestParam(required = false) String fim,
                            @RequestParam(required = false) String tecnico,
                            @RequestHeader(value = "Authorization", required = false) String token,
                            HttpServletResponse response) throws IOException {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            response.sendError(403, "Sem permissao");
            return;
        }

        LocalDateTime ini = parseInicio(inicio, agrupamento);
        LocalDateTime end = parseFim(fim);
        DateTimeFormatter fmt = formatterPara(agrupamento);
        DateTimeFormatter mesFmt = DateTimeFormatter.ofPattern("yyyy-MM");

        var manuts = manutencaoRepository.findAll().stream()
                .filter(m -> m.getDataManutencao() != null)
                .filter(m -> !m.getDataManutencao().isBefore(ini))
                .filter(m -> !m.getDataManutencao().isAfter(end))
                .filter(m -> tecnico == null || tecnico.isBlank()
                        || tecnico.equalsIgnoreCase(m.getTecnicoUsername())
                        || tecnico.equalsIgnoreCase(m.getTecnicoNome()))
                .toList();

        var condList = condenadoRepository.findAll().stream()
                .filter(c -> c.getDataCondenacao() != null)
                .filter(c -> !c.getDataCondenacao().isBefore(ini))
                .filter(c -> !c.getDataCondenacao().isAfter(end))
                .toList();

        Workbook wb = new XSSFWorkbook();
        CellStyle headerStyle = criarHeaderStyle(wb);

        // Aba 1: Manutencoes (serie)
        Sheet s1 = wb.createSheet("Manutencoes " + agrupamento);
        String[] h1 = { agrupamento.toUpperCase(), "Total" };
        criarHeader(s1, h1, headerStyle);
        Map<String, Long> manutAgrup = new TreeMap<>();
        for (var m : manuts) manutAgrup.merge(m.getDataManutencao().format(fmt), 1L, Long::sum);
        int r1 = 1;
        for (var e : manutAgrup.entrySet()) {
            Row row = s1.createRow(r1++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue());
        }
        autoSize(s1, h1.length);

        // Aba 2: Condenados (serie)
        Sheet s2 = wb.createSheet("Condenados " + agrupamento);
        String[] h2 = { agrupamento.toUpperCase(), "Total" };
        criarHeader(s2, h2, headerStyle);
        Map<String, Long> condAgrup = new TreeMap<>();
        for (var c : condList) condAgrup.merge(c.getDataCondenacao().format(fmt), 1L, Long::sum);
        int r2 = 1;
        for (var e : condAgrup.entrySet()) {
            Row row = s2.createRow(r2++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue());
        }
        autoSize(s2, h2.length);

        // Aba 3: Produtividade por tecnico (mes)
        Sheet s3 = wb.createSheet("Produtividade tecnicos");
        String[] h3 = { "Tecnico", "Username", "Mes", "Total" };
        criarHeader(s3, h3, headerStyle);
        Map<String, Map<String, Long>> prod = new TreeMap<>();
        Map<String, String> nomes = new HashMap<>();
        for (var m : manuts) {
            String u = m.getTecnicoUsername() != null ? m.getTecnicoUsername() : "desconhecido";
            nomes.putIfAbsent(u, m.getTecnicoNome() != null ? m.getTecnicoNome() : u);
            prod.computeIfAbsent(u, k -> new TreeMap<>())
                    .merge(m.getDataManutencao().format(mesFmt), 1L, Long::sum);
        }
        int r3 = 1;
        for (var te : prod.entrySet()) {
            for (var me : te.getValue().entrySet()) {
                Row row = s3.createRow(r3++);
                row.createCell(0).setCellValue(nomes.getOrDefault(te.getKey(), te.getKey()));
                row.createCell(1).setCellValue(te.getKey());
                row.createCell(2).setCellValue(me.getKey());
                row.createCell(3).setCellValue(me.getValue());
            }
        }
        autoSize(s3, h3.length);

        // Aba 4: Lista bruta de manutencoes no intervalo
        Sheet s4 = wb.createSheet("Detalhe manutencoes");
        String[] h4 = { "Data", "AgentId", "Tecnico", "Username", "Descricao", "Laudo" };
        criarHeader(s4, h4, headerStyle);
        int r4 = 1;
        for (var m : manuts) {
            Row row = s4.createRow(r4++);
            row.createCell(0).setCellValue(m.getDataManutencao()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            row.createCell(1).setCellValue(nz(m.getAgentId()));
            row.createCell(2).setCellValue(nz(m.getTecnicoNome()));
            row.createCell(3).setCellValue(nz(m.getTecnicoUsername()));
            row.createCell(4).setCellValue(nz(m.getDescricao()));
            row.createCell(5).setCellValue(nz(m.getLaudoFinal()));
        }
        autoSize(s4, h4.length);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fname = "relatorios_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fname + "\"");
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
            wb.close();
        }
    }

    // ===== Helpers =====
    private LocalDateTime parseInicio(String s, String agrupamento) {
        if (s != null && !s.isBlank()) {
            try { return LocalDate.parse(s).atStartOfDay(); } catch (Exception ignore) {}
        }
        // defaults: dia -> 30d, mes -> 12m, ano -> 5y
        if ("ano".equalsIgnoreCase(agrupamento)) return LocalDate.now().minusYears(5).atStartOfDay();
        if ("mes".equalsIgnoreCase(agrupamento)) return LocalDate.now().minusMonths(12).atStartOfDay();
        return LocalDate.now().minusDays(30).atStartOfDay();
    }

    private LocalDateTime parseFim(String s) {
        if (s != null && !s.isBlank()) {
            try { return LocalDate.parse(s).atTime(23, 59, 59); } catch (Exception ignore) {}
        }
        return LocalDate.now().atTime(23, 59, 59);
    }

    private DateTimeFormatter formatterPara(String agrupamento) {
        if ("ano".equalsIgnoreCase(agrupamento)) return DateTimeFormatter.ofPattern("yyyy");
        if ("mes".equalsIgnoreCase(agrupamento)) return DateTimeFormatter.ofPattern("yyyy-MM");
        return DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    private CellStyle criarHeaderStyle(Workbook wb) {
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private void criarHeader(Sheet sheet, String[] headers, CellStyle style) {
        Row h = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(style);
        }
    }

    private void autoSize(Sheet s, int cols) {
        for (int i = 0; i < cols; i++) s.autoSizeColumn(i);
        s.createFreezePane(0, 1);
    }

    private String nz(String s) { return s == null ? "" : s; }
}
