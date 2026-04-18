package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.AuditService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.repository.PerfilRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthControler {

    private final AuthService authService;
    private final AuditService auditService;
    private final PerfilRepository perfilRepository;

    public AuthControler(AuthService authService, AuditService auditService, PerfilRepository perfilRepository) {
        this.authService = authService;
        this.auditService = auditService;
        this.perfilRepository = perfilRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String senha = body.get("senha");

        Optional<Map<String, Object>> resultado = authService.login(username, senha,
                getClientIp(request), request.getHeader("User-Agent"));
        if (resultado.isPresent()) {
            var data = resultado.get();
            if (data.containsKey("error")) {
                auditService.registrar(username, "LOGIN_BLOQUEADO", "/auth/login",
                        "Usuario nao autorizado", getClientIp(request), request.getHeader("User-Agent"));
                return ResponseEntity.status(403).body(Map.of("message", data.get("error")));
            }
            auditService.registrar(username, "LOGIN", "/auth/login",
                    "Login realizado com sucesso", getClientIp(request), request.getHeader("User-Agent"));

            Map<String, Object> resposta = new HashMap<>();
            resposta.put("token", data.get("token"));
            resposta.put("username", data.get("username"));
            resposta.put("perfil", data.get("perfil"));
            resposta.put("nome", data.get("nome"));
            resposta.put("permissoes", data.get("permissoes"));
            resposta.put("message", "Login realizado com sucesso");
            return ResponseEntity.ok(resposta);
        }
        auditService.registrar(username != null ? username : "desconhecido", "LOGIN_FALHA", "/auth/login",
                "Usuario ou senha invalidos", getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.status(401).body(Map.of("message", "Usuario ou senha invalidos"));
    }

    @PostMapping("/verificar")
    public ResponseEntity<?> verificar(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        boolean autorizado = authService.isAutorizado(token);
        if (autorizado) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("autorizado", true);
            resp.put("perfil", authService.getPerfil(token));
            resp.put("username", authService.getUsername(token));
            resp.put("permissoes", authService.getPermissoes(token));
            return ResponseEntity.ok(resp);
        }
        return ResponseEntity.ok(Map.of("autorizado", false));
    }

    @PostMapping("/revalidar")
    public ResponseEntity<?> revalidar(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String senha = body.get("senha");
        boolean valido = authService.revalidar(username, senha);
        if (valido) {
            auditService.registrar(username, "REVALIDACAO", "/auth/revalidar",
                    "Revalidacao para acao critica", getClientIp(request), request.getHeader("User-Agent"));
            return ResponseEntity.ok(Map.of("autorizado", true));
        }
        return ResponseEntity.status(401).body(Map.of("autorizado", false, "message", "Credenciais invalidas"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String token = body.get("token");
        String username = authService.getUsername(token);
        authService.logout(token);
        auditService.registrar(username, "LOGOUT", "/auth/logout",
                "Logout realizado", getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("message", "Logout realizado"));
    }

    // Endpoint para listar perfis disponiveis (usado no formulario de usuario)
    @GetMapping("/perfis")
    public ResponseEntity<?> listarPerfis() {
        var lista = perfilRepository.findAll().stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("nome", p.getNome());
            m.put("descricao", p.getDescricao() != null ? p.getDescricao() : "");
            m.put("acessoUsuarios", p.isAcessoUsuarios());
            m.put("acessoHistorico", p.isAcessoHistorico());
            m.put("acessoSetores", p.isAcessoSetores());
            m.put("acessoRegistro", p.isAcessoRegistro());
            m.put("acessoDelete", p.isAcessoDelete());
            return m;
        }).toList();
        return ResponseEntity.ok(lista);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
