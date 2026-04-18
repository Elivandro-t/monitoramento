package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.AuditService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.repository.UsuarioEntity;
import com.example.projetoAP.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioControler {

    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;
    private final AuditService auditService;

    public UsuarioControler(UsuarioRepository usuarioRepository, AuthService authService, AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Perfil nao tem permissao para acessar"));
        }
        var lista = usuarioRepository.findAll().stream().map(u -> Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "nome", u.getNome() != null ? u.getNome() : "",
                "filial", u.getFilial() != null ? u.getFilial() : "",
                "perfil", u.getPerfil(),
                "setor", u.getSetor() != null ? u.getSetor() : "",
                "cargo", u.getCargo() != null ? u.getCargo() : "",
                "autorizado", u.isAutorizado(),
                "ultimoLogin", u.getUltimoLogin() != null ? u.getUltimoLogin().toString() : ""
        )).toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> body,
                                   @RequestHeader(value = "Authorization", required = false) String token,
                                   HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Perfil nao tem permissao para acessar"));
        }
        String username = (String) body.get("username");
        String senha = (String) body.get("senha");
        if (username == null || senha == null || username.isBlank() || senha.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username e senha sao obrigatorios"));
        }
        if (usuarioRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username ja existe"));
        }

        var usuario = new UsuarioEntity(username, authService.hashSenha(senha));
        usuario.setNome((String) body.getOrDefault("nome", ""));
        usuario.setFilial((String) body.getOrDefault("filial", ""));
        usuario.setPerfil((String) body.getOrDefault("perfil", "INFORMATICA"));
        usuario.setSetor((String) body.getOrDefault("setor", ""));
        usuario.setCargo((String) body.getOrDefault("cargo", ""));
        usuario.setAutorizado(Boolean.TRUE.equals(body.getOrDefault("autorizado", true)));

        usuarioRepository.save(usuario);

        auditService.registrar(authService.getUsername(token), "CRIAR_USUARIO", "/usuarios",
                "Criou usuario: " + username, getClientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("message", "Usuario criado com sucesso"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable long id,
                                    @RequestBody Map<String, Object> body,
                                    @RequestHeader(value = "Authorization", required = false) String token,
                                    HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Perfil nao tem permissao para acessar"));
        }
        var opt = usuarioRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var usuario = opt.get();
        if (body.containsKey("nome")) usuario.setNome((String) body.get("nome"));
        if (body.containsKey("filial")) usuario.setFilial((String) body.get("filial"));
        if (body.containsKey("perfil")) usuario.setPerfil((String) body.get("perfil"));
        if (body.containsKey("setor")) usuario.setSetor((String) body.get("setor"));
        if (body.containsKey("cargo")) usuario.setCargo((String) body.get("cargo"));
        if (body.containsKey("autorizado")) usuario.setAutorizado(Boolean.TRUE.equals(body.get("autorizado")));
        if (body.containsKey("senha")) {
            String novaSenha = (String) body.get("senha");
            if (novaSenha != null && !novaSenha.isBlank()) {
                usuario.setSenha(authService.hashSenha(novaSenha));
            }
        }

        usuarioRepository.save(usuario);

        auditService.registrar(authService.getUsername(token), "EDITAR_USUARIO", "/usuarios/" + id,
                "Editou usuario: " + usuario.getUsername(), getClientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("message", "Usuario atualizado com sucesso"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@PathVariable long id,
                                     @RequestHeader(value = "Authorization", required = false) String token,
                                     HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Perfil nao tem permissao para acessar"));
        }
        var opt = usuarioRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String usernameDeleted = opt.get().getUsername();
        usuarioRepository.deleteById(id);

        auditService.registrar(authService.getUsername(token), "DELETAR_USUARIO", "/usuarios/" + id,
                "Deletou usuario: " + usernameDeleted, getClientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("message", "Usuario excluido com sucesso"));
    }

    @PatchMapping("/{id}/autorizar")
    public ResponseEntity<?> toggleAutorizacao(@PathVariable long id,
                                               @RequestHeader(value = "Authorization", required = false) String token,
                                               HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Perfil nao tem permissao para acessar"));
        }
        var opt = usuarioRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var usuario = opt.get();
        usuario.setAutorizado(!usuario.isAutorizado());
        usuarioRepository.save(usuario);

        String acao = usuario.isAutorizado() ? "AUTORIZAR_USUARIO" : "DESAUTORIZAR_USUARIO";
        auditService.registrar(authService.getUsername(token), acao, "/usuarios/" + id + "/autorizar",
                "Usuario: " + usuario.getUsername() + " -> autorizado: " + usuario.isAutorizado(),
                getClientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("message", "Status atualizado", "autorizado", usuario.isAutorizado()));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
