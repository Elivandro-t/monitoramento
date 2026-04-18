package com.example.projetoAP.controler;

import com.example.projetoAP.controler.SetoresService.SetorService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.controler.service.GroupService;
import com.example.projetoAP.domain.vo.GroupDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("group")
@CrossOrigin(origins = "*")
public class GroupControler {
    GroupService groupService;
    SetorService setorService;
    AuthService authService;

    public GroupControler(GroupService service, SetorService setorService, AuthService authService) {
        this.groupService = service;
        this.setorService = setorService;
        this.authService = authService;
    }

    @GetMapping("/registro")
    public String getSave(Model model) {
        model.addAttribute("grupos", groupService.findAll());
        model.addAttribute("setores", setorService.getLista());
        return "group";
    }

    @PostMapping("/registro")
    public String setSave(GroupDTO groupDTO, Model model) {
        groupService.salve(groupDTO);
        model.addAttribute("mensagem", "Grupo registrado com sucesso");
        model.addAttribute("grupos", groupService.findAll());
        model.addAttribute("setores", setorService.getLista());
        return "group";
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deletarGrupo(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.isAutorizado(token)) {
            return ResponseEntity.status(401).body(Map.of("message", "Sessao expirada. Faca login novamente."));
        }
        groupService.deletar(id);
        return ResponseEntity.ok(Map.of("message", "Grupo excluido com sucesso"));
    }
}
