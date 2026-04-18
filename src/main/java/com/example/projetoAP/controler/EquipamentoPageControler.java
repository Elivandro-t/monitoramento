package com.example.projetoAP.controler;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/endpoint")
public class EquipamentoPageControler {

    @GetMapping("/equipamentos")
    public String equipamentos() {
        return "equipamentos";
    }

    @GetMapping("/monitoramento/equipamentos")
    public String monitorEquipamentos() {
        return "monitor-equipamentos";
    }
}
