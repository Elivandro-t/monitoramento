package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.AuditService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.controler.service.EquipamentoCondenacaoService;
import com.example.projetoAP.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/equipamentos-condenados")
@CrossOrigin(origins = "*")
@Tag(name = "Equipamentos Condenados", description = "Historico de equipamentos eliminados/descartados")
public class EquipamentoCondenadoController {

    private final EquipamentoCondenadoRepository repository;
    private final AgenteRepository agenteRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final AuthService authService;
    private final AuditService auditService;
    private final EquipamentoCondenacaoService condenacaoService;
    private final ObjectMapper mapper = new ObjectMapper();

    public EquipamentoCondenadoController(EquipamentoCondenadoRepository repository,
                                          AgenteRepository agenteRepository,
                                          ManutencaoRepository manutencaoRepository,
                                          AuthService authService,
                                          AuditService auditService,
                                          EquipamentoCondenacaoService condenacaoService) {
        this.repository = repository;
        this.agenteRepository = agenteRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.condenacaoService = condenacaoService;
    }

    @Operation(summary = "Lista todos os equipamentos condenados (historico)")
    @GetMapping
    public ResponseEntity<?> listar(@RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var lista = repository.findAllByOrderByDataCondenacaoDesc().stream()
                .map(this::toMap).toList();
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Detalhes de um equipamento condenado")
    @GetMapping("/{id}")
    public ResponseEntity<?> detalhe(@PathVariable long id,
                                     @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var opt = repository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toMapDetalhado(opt.get()));
    }

    /**
     * Condena/elimina um equipamento: copia snapshot completo (inclusive historico de
     * manutencoes) para a tabela de condenados, e remove o agente original + manutencoes.
     */
    @Operation(summary = "Condena (elimina) um equipamento, preservando historico em tabela separada")
    @PostMapping("/condenar/{agenteId}")
    @Transactional
    public ResponseEntity<?> condenar(@PathVariable long agenteId,
                                      @RequestBody Map<String, String> body,
                                      @RequestHeader(value = "Authorization", required = false) String token,
                                      HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoDelete")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var opt = agenteRepository.findById(agenteId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var ag = opt.get();
        String motivo = body != null ? body.getOrDefault("motivo", "") : "";
        String laudoTexto = body != null ? body.get("laudoFinal") : null;

        try {
            condenacaoService.condenar(ag, motivo, laudoTexto, authService.getUsername(token));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }

        auditService.registrar(authService.getUsername(token), "CONDENAR_EQUIPAMENTO",
                "/equipamentos-condenados/condenar/" + agenteId,
                "Condenou: " + ag.getAgentId(), getClientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("message", "Equipamento condenado e movido para o historico"));
    }

    @Operation(summary = "Remove permanentemente um equipamento do historico de condenados")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deletar(@PathVariable long id,
                                     @RequestHeader(value = "Authorization", required = false) String token,
                                     HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoDelete")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        auditService.registrar(authService.getUsername(token), "DELETAR_CONDENADO",
                "/equipamentos-condenados/" + id,
                "Removeu condenado ID: " + id, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("message", "Registro removido permanentemente"));
    }

    private Map<String, Object> toMap(EquipamentoCondenadoEntity c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("agentId", nz(c.getAgentId()));
        m.put("hostname", nz(c.getHostname()));
        m.put("patrimonio", nz(c.getPatrimonio()));
        m.put("marca", nz(c.getMarca()));
        m.put("modelo", nz(c.getModelo()));
        m.put("numeroSerie", nz(c.getNumeroSerie()));
        m.put("setor", nz(c.getSetor()));
        m.put("filial", nz(c.getFilial()));
        m.put("tipo", nz(c.getTipo()));
        m.put("motivoCondenacao", nz(c.getMotivoCondenacao()));
        m.put("totalManutencoes", c.getTotalManutencoes() != null ? c.getTotalManutencoes() : 0);
        m.put("condenadoPor", nz(c.getCondenadoPor()));
        m.put("dataCondenacao", c.getDataCondenacao() != null ?
                c.getDataCondenacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
        return m;
    }

    private Map<String, Object> toMapDetalhado(EquipamentoCondenadoEntity c) {
        Map<String, Object> m = toMap(c);
        m.put("laudoFinal", nz(c.getLaudoFinal()));
        List<Map<String, Object>> historico;
        try {
            historico = mapper.readValue(c.getHistoricoManutencoes() == null ? "[]" : c.getHistoricoManutencoes(),
                    List.class);
        } catch (Exception e) { historico = new ArrayList<>(); }
        m.put("historicoManutencoes", historico);
        return m;
    }

    private String nz(String s) { return s == null ? "" : s; }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isEmpty()) ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }
}
