package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.AuditService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/agentes")
@CrossOrigin(origins = "*")
public class AgenteControler {

    private final AgenteRepository agenteRepository;
    private final AgenteMetricaRepository metricaRepository;
    private final AuthService authService;
    private final AuditService auditService;
    private final SimpMessagingTemplate messagingTemplate;

    public AgenteControler(AgenteRepository agenteRepository,
                           AgenteMetricaRepository metricaRepository,
                           AuthService authService,
                           AuditService auditService,
                           SimpMessagingTemplate messagingTemplate) {
        this.agenteRepository = agenteRepository;
        this.metricaRepository = metricaRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.messagingTemplate = messagingTemplate;
    }

    // ===== ENDPOINT QUE O AGENTE CHAMA PARA ENVIAR METRICAS =====
    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat(@RequestBody Map<String, Object> body) {
        String agentId = (String) body.get("agentId");
        if (agentId == null || agentId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "agentId obrigatorio"));
        }

        var opt = agenteRepository.findByAgentId(agentId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Agente nao cadastrado"));
        }

        var agente = opt.get();
        agente.setAtivo(true);
        agente.setStatus("ONLINE");
        agente.setUltimoContato(LocalDateTime.now());

        if (body.containsKey("cpu")) agente.setCpuPercent(toDouble(body.get("cpu")));
        if (body.containsKey("memoriaPercent")) agente.setMemoriaPercent(toDouble(body.get("memoriaPercent")));
        if (body.containsKey("memoriaTotal")) agente.setMemoriaTotal(toLong(body.get("memoriaTotal")));
        if (body.containsKey("memoriaUsada")) agente.setMemoriaUsada(toLong(body.get("memoriaUsada")));
        if (body.containsKey("discoTotal")) agente.setDiscoTotal(toLong(body.get("discoTotal")));
        if (body.containsKey("discoUsado")) agente.setDiscoUsado(toLong(body.get("discoUsado")));
        if (body.containsKey("hostname")) agente.setHostname((String) body.get("hostname"));
        if (body.containsKey("ip")) agente.setIp((String) body.get("ip"));
        if (body.containsKey("so")) agente.setSistemaOperacional((String) body.get("so"));
        if (body.containsKey("usuario")) agente.setUsuario((String) body.get("usuario"));

        agenteRepository.save(agente);

        // Salvar metrica historica
        metricaRepository.save(new AgenteMetricaEntity(
                agentId, agente.getCpuPercent(), agente.getMemoriaPercent(),
                agente.getMemoriaUsada(), "ONLINE"));

        // Notificar via WebSocket
        messagingTemplate.convertAndSend("/topic/agentes", Map.of("tipo", "heartbeat", "agentId", agentId));

        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    // ===== LISTAR AGENTES =====
    @GetMapping
    public ResponseEntity<?> listar(@RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var lista = agenteRepository.findAll().stream().map(this::toMap).toList();
        return ResponseEntity.ok(lista);
    }

    // ===== CADASTRAR AGENTE =====
    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody Map<String, String> body,
                                       @RequestHeader(value = "Authorization", required = false) String token,
                                       HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        String agentId = body.get("agentId");
        if (agentId == null || agentId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "ID do agente obrigatorio"));
        }
        if (agenteRepository.findByAgentId(agentId).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Agente ja cadastrado"));
        }

        var agente = new AgenteEntity(agentId,
                body.getOrDefault("hostname", ""),
                body.getOrDefault("setor", ""),
                body.getOrDefault("filial", ""),
                body.getOrDefault("anydesk", ""),
                body.getOrDefault("dominio", ""));

        agenteRepository.save(agente);

        auditService.registrar(authService.getUsername(token), "CADASTRAR_AGENTE", "/agentes",
                "Cadastrou agente: " + agentId, getClientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("message", "Agente cadastrado com sucesso"));
    }

    // ===== EDITAR AGENTE =====
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable long id,
                                    @RequestBody Map<String, String> body,
                                    @RequestHeader(value = "Authorization", required = false) String token,
                                    HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var opt = agenteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        var ag = opt.get();
        if (body.containsKey("hostname")) ag.setHostname(body.get("hostname"));
        if (body.containsKey("setor")) ag.setSetor(body.get("setor"));
        if (body.containsKey("filial")) ag.setFilial(body.get("filial"));
        if (body.containsKey("anydesk")) ag.setAnydesk(body.get("anydesk"));
        if (body.containsKey("dominio")) ag.setDominio(body.get("dominio"));
        agenteRepository.save(ag);

        auditService.registrar(authService.getUsername(token), "EDITAR_AGENTE", "/agentes/" + id,
                "Editou agente: " + ag.getAgentId(), getClientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("message", "Agente atualizado"));
    }

    // ===== DELETAR AGENTE =====
    @DeleteMapping("/{id}")
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
    @GetMapping("/{id}")
    public ResponseEntity<?> detalhe(@PathVariable long id,
                                     @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var opt = agenteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toMap(opt.get()));
    }

    // ===== METRICAS HISTORICAS =====
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
                    m.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "");
            return map;
        }).toList();

        return ResponseEntity.ok(lista);
    }

    // ===== SESSOES ATIVAS =====
    @GetMapping("/sessoes")
    public ResponseEntity<?> sessoesAtivas(@RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        return ResponseEntity.ok(authService.getSessoesAtivas());
    }

    // ===== HISTORICO SESSOES =====
    @GetMapping("/sessoes/historico")
    public ResponseEntity<?> historicoSessoes(@RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoHistorico")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        return ResponseEntity.ok(authService.getHistoricoSessoes());
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

    private Map<String, Object> toMap(AgenteEntity a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("agentId", a.getAgentId());
        m.put("hostname", a.getHostname() != null ? a.getHostname() : "");
        m.put("ip", a.getIp() != null ? a.getIp() : "");
        m.put("setor", a.getSetor() != null ? a.getSetor() : "");
        m.put("filial", a.getFilial() != null ? a.getFilial() : "");
        m.put("anydesk", a.getAnydesk() != null ? a.getAnydesk() : "");
        m.put("dominio", a.getDominio() != null ? a.getDominio() : "");
        m.put("so", a.getSistemaOperacional() != null ? a.getSistemaOperacional() : "");
        m.put("usuario", a.getUsuario() != null ? a.getUsuario() : "");
        m.put("ativo", a.isAtivo());
        m.put("status", a.getStatus() != null ? a.getStatus() : "OFFLINE");
        m.put("cpu", a.getCpuPercent() != null ? a.getCpuPercent() : 0);
        m.put("memoriaPercent", a.getMemoriaPercent() != null ? a.getMemoriaPercent() : 0);
        m.put("memoriaTotal", a.getMemoriaTotal() != null ? a.getMemoriaTotal() : 0);
        m.put("memoriaUsada", a.getMemoriaUsada() != null ? a.getMemoriaUsada() : 0);
        m.put("discoTotal", a.getDiscoTotal() != null ? a.getDiscoTotal() : 0);
        m.put("discoUsado", a.getDiscoUsado() != null ? a.getDiscoUsado() : 0);
        m.put("ultimoContato", a.getUltimoContato() != null ?
                a.getUltimoContato().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
        m.put("dataCadastro", a.getDataCadastro() != null ?
                a.getDataCadastro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
        return m;
    }

    private Double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0.0; }
    }

    private Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isEmpty()) ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }
}
