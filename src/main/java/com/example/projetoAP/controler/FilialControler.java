package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.AuditService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.repository.FilialEntity;
import com.example.projetoAP.repository.FilialRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/filiais")
@CrossOrigin(origins = "*")
@Tag(name = "Filiais", description = "CRUD de filiais (unidades/CDs) do grupo")
public class FilialControler {

    private final FilialRepository filialRepository;
    private final AuthService authService;
    private final AuditService auditService;

    public FilialControler(FilialRepository filialRepository,
                           AuthService authService,
                           AuditService auditService) {
        this.filialRepository = filialRepository;
        this.authService = authService;
        this.auditService = auditService;
    }

    @Operation(summary = "Lista todas as filiais cadastradas")
    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(filialRepository.findAll().stream().map(this::toMap).toList());
    }

    @Operation(summary = "Cria uma nova filial (requer permissao acessoUsuarios)")
    @PostMapping
    @Transactional
    public ResponseEntity<?> criar(@RequestBody Map<String, String> body,
                                   @RequestHeader(value = "Authorization", required = false) String token,
                                   HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        String nome = body.get("nome");
        if (nome == null || nome.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nome obrigatorio"));
        }
        if (filialRepository.findByNome(nome).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Filial ja existe"));
        }
        var f = new FilialEntity(nome, body.get("codigo"), body.get("cidade"), body.get("estado"));
        filialRepository.save(f);
        auditService.registrar(authService.getUsername(token), "CRIAR_FILIAL", "/filiais",
                "Filial: " + nome, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("message", "Filial criada", "id", f.getId()));
    }

    @Operation(summary = "Atualiza os dados de uma filial existente")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable long id,
                                    @RequestBody Map<String, String> body,
                                    @RequestHeader(value = "Authorization", required = false) String token,
                                    HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var opt = filialRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var f = opt.get();
        if (body.containsKey("nome")) f.setNome(body.get("nome"));
        if (body.containsKey("codigo")) f.setCodigo(body.get("codigo"));
        if (body.containsKey("cidade")) f.setCidade(body.get("cidade"));
        if (body.containsKey("estado")) f.setEstado(body.get("estado"));
        filialRepository.save(f);
        auditService.registrar(authService.getUsername(token), "EDITAR_FILIAL", "/filiais/" + id,
                "Filial: " + id, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("message", "Filial atualizada"));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deletar(@PathVariable long id,
                                     @RequestHeader(value = "Authorization", required = false) String token,
                                     HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoDelete")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        if (!filialRepository.existsById(id)) return ResponseEntity.notFound().build();
        filialRepository.deleteById(id);
        auditService.registrar(authService.getUsername(token), "DELETAR_FILIAL", "/filiais/" + id,
                "Filial: " + id, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("message", "Filial excluida"));
    }

    private Map<String, Object> toMap(FilialEntity f) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", f.getId());
        m.put("nome", f.getNome() != null ? f.getNome() : "");
        m.put("codigo", f.getCodigo() != null ? f.getCodigo() : "");
        m.put("cidade", f.getCidade() != null ? f.getCidade() : "");
        m.put("estado", f.getEstado() != null ? f.getEstado() : "");
        return m;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isEmpty()) ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }
}
