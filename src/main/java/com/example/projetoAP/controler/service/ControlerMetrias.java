package com.example.projetoAP.controler.service;

import com.example.projetoAP.domain.vo.IcmpDTO;
import com.example.projetoAP.domain.vo.MetricasDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping
public class ControlerMetrias {
    @Autowired
    CalcularPerdas calcularPerdas;
    ItensMetricasService service;
    public  ControlerMetrias(ItensMetricasService itensMetricasService){
        this.service = itensMetricasService;
    }
    @PostMapping("/inform")
    public CompletableFuture<Map<String,List<MetricasDTO>>> lista(@RequestBody Filter id){
        System.out.print("Dados vindo do banckend "+id);
        return calcularPerdas.listaMetricas(id);
    }
    @GetMapping("/itens")
    public Map<String,List<IcmpDTO>> lista(){
        return service.lista();
    }
}
