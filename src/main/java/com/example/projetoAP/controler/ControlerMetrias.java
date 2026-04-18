package com.example.projetoAP.controler;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.controler.service.*;
import com.example.projetoAP.domain.vo.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping
@CrossOrigin(origins = "*")
public class ControlerMetrias {
    @Autowired
    CalcularPerdas calcularPerdas;
    ItensMetricasService service;
    GroupService groupService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    EndpointService endpointService;
    public  ControlerMetrias(ItensMetricasService itensMetricasService,EndpointService endpointService,GroupService groupService){

        this.service = itensMetricasService;
        this.endpointService =endpointService;
        this.groupService = groupService;
    }
    @MessageMapping("/enviarDados")
    public  void lista( Filter id){
      calcularPerdas.listaMetricas(id).thenAccept(result->{
          messagingTemplate.convertAndSend("/grafico",result);
      });
    }
    @MessageMapping("/metricassss")
    public void listaMestricas1(@Payload  FilterHost filter){
        Map<String, List<MetricasDTO>> future = calcularPerdas.ListaMestricasSemID(filter);
        messagingTemplate.convertAndSend("/topic/grafico/" + filter.clientId(), future);

    };

    // Guarda 'in-flight' por (clientId + hostid) para evitar empilhar envios
    // quando o cliente ainda nao consumiu o anterior.
    private final ConcurrentHashMap<String, AtomicBoolean> emVoo = new ConcurrentHashMap<>();

    @MessageMapping("/metricas")
    public void listaMestricas(@Payload FilterHost filter) {
        String key = filter.clientId() + ":" + filter.hostid();
        AtomicBoolean flag = emVoo.computeIfAbsent(key, k -> new AtomicBoolean(false));
        if (!flag.compareAndSet(false, true)) {
            // ja existe um envio em andamento para esse cliente/host; descarta este
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, List<MetricasDTO>> future = calcularPerdas.ListaMestricasSemID(filter);
                messagingTemplate.convertAndSend("/topic/grafico/" + filter.clientId() + "/" + filter.hostid(), future);
            } finally {
                flag.set(false);
            }
        });
    }

    @MessageMapping("/cmd")
    public  void  CMD(@RequestBody  @Valid cmdDTO dto) throws IOException {
        new Thread(() -> {
            try {
                endpointService.CMDPing(dto);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();




    }
    @MessageMapping("/mestricasM")
    public void getVierListPageSocket(@Payload Dados receiveGrupoId){
        List<EndpointDTO> l = new ArrayList<>();
        if(receiveGrupoId.ReceiveGrupoId()!=null && !receiveGrupoId.ReceiveGrupoId().isEmpty()){
            var lista = endpointService.getLista().stream()
                    .filter(e -> e.grupo().equals(receiveGrupoId.ReceiveGrupoId())).toList();
           l.addAll(lista);
        }
        else {
            l.addAll(endpointService.getLista());
            // Caso o MAC seja null, adiciona a lista completa
        };
        messagingTemplate.convertAndSend("/topic/monitor/"+receiveGrupoId.clientId(),l);

//       messagingTemplate.convertAndSend("/lista",listMap);
    }
    @PostMapping("/metricas")
    public Map<String,List<MetricasDTO>> listaMestricasApi(@RequestBody FilterHost filter){
        return calcularPerdas.ListaMestricasSemID(filter);
    }
    @GetMapping("/itens")
    public Map<String,List<IcmpDTO>> lista(){
        return service.lista();
    }
    @GetMapping("/itens/group")
    public List<GroupRegistroDTO> findAll(){
        var lista = groupService.findAll();
        return lista;
    }
}
