package com.example.projetoAP.controler.SetorControler;

import com.example.projetoAP.controler.SetoresService.SetorService;
import com.example.projetoAP.controler.service.AuditService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.domain.model.Setor.SetorRegistroDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@CrossOrigin(origins = "*")
@RequestMapping
public class SetorControler {
    SetorService setorService;
    AuthService authService;
    AuditService auditService;

    public SetorControler(SetorService service, AuthService authService, AuditService auditService) {
        this.setorService = service;
        this.authService = authService;
        this.auditService = auditService;
    }

    @GetMapping("/setores")
    public String getRegistro(Model model) {
        model.addAttribute("lista", setorService.getLista());
        return "setores";
    }

    @PostMapping("/setores")
    @Transactional
    public String setRegistro(SetorRegistroDTO s, RedirectAttributes redirectAttributes) {
        setorService.setRegistro(s);
        redirectAttributes.addFlashAttribute("mensagem", "Setor registrado com sucesso");
        return "redirect:/setores";
    }

    @DeleteMapping("/setores/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deletarSetor(@PathVariable Long id,
                                           @RequestHeader(value = "Authorization", required = false) String token,
                                           HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoDelete")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao para deletar."));
        }
        String username = authService.getUsername(token);
        setorService.deletar(id);
        auditService.registrar(username, "DELETAR_SETOR", "/setores/delete/" + id,
                "Deletou setor ID: " + id, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("message", "Setor excluido com sucesso"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/setores/lista")
    public String getLista(Model model) {
        model.addAttribute("lista", setorService.getLista());
        return "setores";
    }
}
