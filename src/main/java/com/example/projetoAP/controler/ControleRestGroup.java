package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.GroupService;
import com.example.projetoAP.ultios.GroupAPIDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("group")
@CrossOrigin(origins = "*")
public class ControleRestGroup {
    GroupService groupService;
    public ControleRestGroup(GroupService service){
        this.groupService = service;
    }
    @GetMapping("/lista_item")
    public ResponseEntity<List<GroupAPIDTO>> getSave(Model model){

        var lista = groupService.findAll().stream().map(GroupAPIDTO::new).toList();
        return  ResponseEntity.ok(lista);
    }

}


