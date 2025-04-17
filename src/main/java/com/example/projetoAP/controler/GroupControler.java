package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.GroupService;
import com.example.projetoAP.domain.vo.GroupDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("group")
@CrossOrigin(origins = "*")
public class GroupControler {
    GroupService groupService;
    public GroupControler(GroupService service){
        this.groupService = service;
    }
    @GetMapping("/registro")
    public String getSave( Model model){
        return "group";
    }
    @PostMapping("/registro")
    public String setSave(GroupDTO groupDTO,Model model){
        groupService.salve(groupDTO);
        model.addAttribute("mensagem","Registrado");

        return "group";
    }
}
