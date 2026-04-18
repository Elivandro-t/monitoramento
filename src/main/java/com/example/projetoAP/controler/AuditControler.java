package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.AuditService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.repository.AuditLogEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit")
@CrossOrigin(origins = "*")
public class AuditControler {

    private final AuditService auditService;
    private final AuthService authService;

    public AuditControler(AuditService auditService, AuthService authService) {
        this.auditService = auditService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestHeader(value = "Authorization", required = false) String token,
                                    @RequestParam(required = false) String usuario,
                                    @RequestParam(required = false) String acao,
                                    @RequestParam(required = false) String dataInicio,
                                    @RequestParam(required = false) String dataFim) {
        if (!authService.temPermissao(token, "acessoHistorico")) {
            return ResponseEntity.status(403).body(Map.of("message", "Perfil nao tem permissao para acessar"));
        }

        LocalDateTime inicio = null;
        LocalDateTime fim = null;
        if (dataInicio != null && !dataInicio.isBlank()) {
            inicio = LocalDate.parse(dataInicio).atStartOfDay();
        }
        if (dataFim != null && !dataFim.isBlank()) {
            fim = LocalDate.parse(dataFim).atTime(23, 59, 59);
        }

        List<AuditLogEntity> logs = auditService.filtrar(usuario, acao, inicio, fim);

        var resultado = logs.stream().map(l -> Map.of(
                "id", l.getId(),
                "usuario", l.getUsuario() != null ? l.getUsuario() : "",
                "acao", l.getAcao() != null ? l.getAcao() : "",
                "rota", l.getRota() != null ? l.getRota() : "",
                "dados", l.getDados() != null ? l.getDados() : "",
                "timestamp", l.getTimestamp() != null ? l.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "",
                "ipMaquina", l.getIpMaquina() != null ? l.getIpMaquina() : "",
                "navegador", l.getNavegador() != null ? l.getNavegador() : "",
                "sistemaOperacional", l.getSistemaOperacional() != null ? l.getSistemaOperacional() : ""
        )).toList();

        return ResponseEntity.ok(resultado);
    }
}
