package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.AuditService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.dto.MTsetorDTO;
import com.example.projetoAP.repository.MTsetor;
import com.example.projetoAP.repository.MTsetorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mtsetores")
@CrossOrigin(origins = "*")
@Tag(name = "MTsetor", description = "CRUD de setores de equipamentos (independente de /setores)")
public class MTsetorController {

    private final MTsetorRepository repository;
    private final AuthService authService;
    private final AuditService auditService;

    public MTsetorController(MTsetorRepository repository, AuthService authService, AuditService auditService) {
        this.repository = repository;
        this.authService = authService;
        this.auditService = auditService;
    }

    @Operation(summary = "Lista todos os MTsetores cadastrados")
    @GetMapping
    public ResponseEntity<List<MTsetorDTO>> listar() {
        var lista = repository.findAll().stream()
                .map(e -> new MTsetorDTO(e.getId(), e.getNome(), e.getFilial(), e.getDescricao()))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Busca um MTsetor pelo id")
    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id) {
        var opt = repository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var e = opt.get();
        return ResponseEntity.ok(new MTsetorDTO(e.getId(), e.getNome(), e.getFilial(), e.getDescricao()));
    }

    @Operation(summary = "Cria um MTsetor")
    @PostMapping
    @Transactional
    public ResponseEntity<?> criar(@RequestBody MTsetorDTO body,
                                   @RequestHeader(value = "Authorization", required = false) String token,
                                   HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        if (body.getNome() == null || body.getNome().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nome obrigatorio"));
        }
        if (repository.findByNome(body.getNome()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "MTsetor ja existe"));
        }
        MTsetor novo = new MTsetor(body.getNome(), body.getFilial(), body.getDescricao());
        repository.save(novo);
        auditService.registrar(authService.getUsername(token), "CRIAR_MTSETOR", "/mtsetores",
                "Criou MTsetor: " + novo.getNome(), getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(new MTsetorDTO(novo.getId(), novo.getNome(), novo.getFilial(), novo.getDescricao()));
    }

    @Operation(summary = "Edita um MTsetor")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable Long id,
                                    @RequestBody MTsetorDTO body,
                                    @RequestHeader(value = "Authorization", required = false) String token,
                                    HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var opt = repository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var e = opt.get();
        if (body.getNome() != null) e.setNome(body.getNome());
        if (body.getFilial() != null) e.setFilial(body.getFilial());
        if (body.getDescricao() != null) e.setDescricao(body.getDescricao());
        repository.save(e);
        auditService.registrar(authService.getUsername(token), "EDITAR_MTSETOR", "/mtsetores/" + id,
                "Editou MTsetor: " + e.getNome(), getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(new MTsetorDTO(e.getId(), e.getNome(), e.getFilial(), e.getDescricao()));
    }

    @Operation(summary = "Remove um MTsetor")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deletar(@PathVariable Long id,
                                     @RequestHeader(value = "Authorization", required = false) String token,
                                     HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoDelete")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        auditService.registrar(authService.getUsername(token), "DELETAR_MTSETOR", "/mtsetores/" + id,
                "Deletou MTsetor ID: " + id, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("message", "MTsetor excluido"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isEmpty()) ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }
}
