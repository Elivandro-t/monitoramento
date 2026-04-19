package com.example.projetoAP.controler;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class EquipamentoPageControler {

    @GetMapping("/endpoint/equipamentos")
    public String equipamentos() {
        return "equipamentos";
    }

    @GetMapping("/endpoint/monitoramento/equipamentos")
    public String monitorEquipamentos() {
        return "monitor-equipamentos";
    }

    @GetMapping("/manutencoes")
    public String manutencoesPage() {
        return "manutencoes";
    }

    @GetMapping("/equipamentos/condenados")
    public String condenadosPage() {
        return "equipamentos_condenados";
    }

    @GetMapping("/relatorios/ranking")
    public String rankingPage() {
        return "ranking";
    }

    @GetMapping("/relatorios/metricas")
    public String metricasPage() {
        return "metricas";
    }
}
