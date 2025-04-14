package com.example.projetoAP.controler;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.controler.service.CalcularPerdas;
import com.example.projetoAP.controler.service.EndpointService;
import com.example.projetoAP.controler.service.ItensMetricasService;
import com.example.projetoAP.domain.vo.Filter;
import com.example.projetoAP.domain.vo.FilterHost;
import com.example.projetoAP.domain.vo.IcmpDTO;
import com.example.projetoAP.domain.vo.MetricasDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
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
    EndpointService endpointService;
    public  ControlerMetrias(ItensMetricasService itensMetricasService,EndpointService endpointService){

        this.service = itensMetricasService;
        this.endpointService =endpointService;
    }
    @MessageMapping("/enviarDados")
    public  void lista( Filter id){
      calcularPerdas.listaMetricas(id).thenAccept(result->{
          messagingTemplate.convertAndSend("/grafico",result);
      });
    }
    @MessageMapping("/metricas")
    @SendTo("/grafico")
    public CompletableFuture<Map<String,List<MetricasDTO>>> listaMestricas(FilterHost filter){
        return calcularPerdas.ListaMestricasSemID(filter);
    };
    @MessageMapping("/mestricasM")
    public void getVierListPageSocket( String ReceiveGrupoId){
        List<EndpointDTO> l = new ArrayList<>();
        if(ReceiveGrupoId!=null && !ReceiveGrupoId.isEmpty()){
            System.out.print("Meus dado vindo do front"+ReceiveGrupoId);

            var lista = endpointService.getLista().stream()
                    .filter(e -> e.grupo().contains(ReceiveGrupoId)).toList();
           l.addAll(lista);
        }
        else {
            l.addAll(endpointService.getLista());
            // Caso o MAC seja null, adiciona a lista completa
        };
        messagingTemplate.convertAndSend("/lista",l);

//       messagingTemplate.convertAndSend("/lista",listMap);
    }
    @PostMapping("/metricas")
    public CompletableFuture<Map<String,List<MetricasDTO>>> listaMestricasApi(@RequestBody FilterHost filter){
        return calcularPerdas.ListaMestricasSemID(filter);
    }
    @GetMapping("/itens")
    public Map<String,List<IcmpDTO>> lista(){
        return service.lista();
    }
}
