package com.example.projetoAP.infra;

import com.example.projetoAP.controler.service.AuthService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Bloqueia acesso ao Swagger UI e a documentacao OpenAPI
 * sem um token valido com permissao "acessoUsuarios".
 * O token pode vir por header Authorization OU query param ?token=...
 * (util para abrir o Swagger em nova aba).
 */
@Component
public class SwaggerAccessFilter implements Filter {

    private final AuthService authService;

    public SwaggerAccessFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        if (isProtected(uri)) {
            String token = req.getHeader("Authorization");
            if (token == null || token.isBlank()) {
                token = req.getParameter("token");
            }
            if (!authService.temPermissao(token, "acessoUsuarios")) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType("text/html;charset=UTF-8");
                res.getWriter().write(htmlAcessoNegado());
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isProtected(String uri) {
        if (uri == null) return false;
        return uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.equals("/swagger-ui.html");
    }

    private String htmlAcessoNegado() {
        return "<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\">"
                + "<title>Acesso Negado</title>"
                + "<style>body{background:#0d1117;color:#c9d1d9;font-family:'Segoe UI',sans-serif;"
                + "display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;margin:0;}"
                + "h1{color:#f85149;font-size:22px;}p{color:#8b949e;font-size:13px;}"
                + "a{color:#58a6ff;text-decoration:none;margin-top:16px;border:1px solid #30363d;padding:8px 16px;border-radius:6px;}"
                + "a:hover{background:#30363d;}</style></head><body>"
                + "<h1>403 - Acesso Negado</h1>"
                + "<p>Voce precisa estar logado com permissao de usuario para acessar a documentacao.</p>"
                + "<a href=\"/endpoint/monitoramento\">Voltar ao sistema</a>"
                + "</body></html>";
    }
}
