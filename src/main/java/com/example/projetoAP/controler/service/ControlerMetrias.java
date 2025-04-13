package com.example.projetoAP.controler.service;

import com.example.projetoAP.domain.vo.IcmpDTO;
import com.example.projetoAP.domain.vo.MetricasDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    public  ControlerMetrias(ItensMetricasService itensMetricasService){
        this.service = itensMetricasService;
    }
    @MessageMapping("/enviarDados")
    public  void lista( Filter id){
      calcularPerdas.listaMetricas(id).thenAccept(result->{
          messagingTemplate.convertAndSend("/grafico",result);

      });
    }
    @PostMapping("/mestricasHost")
    public CompletableFuture<Map<String,List<MetricasDTO>>> listaMestricas(@RequestBody FilterHost filter){
        return calcularPerdas.ListaMestricasSemID(filter);
    }
    @GetMapping("/itens")
    public Map<String,List<IcmpDTO>> lista(){
        return service.lista();
    }
}
