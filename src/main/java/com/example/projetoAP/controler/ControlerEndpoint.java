package com.example.projetoAP.controler;
import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Service.MainService;
import com.example.projetoAP.controler.service.CalcularPerdas;
import com.example.projetoAP.controler.service.EndepointBuscadosService;
import com.example.projetoAP.domain.vo.MetricasDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("endpoint")
public class ControlerEndpoint {
    @Autowired
    private MainService macConectados;
    private EndepointBuscadosService endepoint;
    List<EndpointDTO> listas;
    public ControlerEndpoint(EndepointBuscadosService endepointBuscadosService){
        this.listas = new ArrayList<>();
        this.endepoint = endepointBuscadosService;
    }
    public ControlerEndpoint(){};
    @GetMapping("/index/{mac}")
    public ResponseEntity<List<EndpointDTO>> getList(@PathVariable String mac) throws Exception {
        var listass  = macConectados.allEndpoints.stream().filter(e->e.mac().equals(mac)).toList();

        return ResponseEntity.ok(listass);
    }
    @GetMapping("/item")
    public ResponseEntity<List<EndpointDTO>> getOnt() throws Exception {
        var lista  = macConectados.Dev();
        lista.addAll(lista);
        return ResponseEntity.ok(lista);
    }
    @GetMapping("/item/lista")
    public ResponseEntity<List<EndpointDTO>> gel() throws Exception {

        return ResponseEntity.ok(listas);
    }
    @GetMapping("/mac/{mac}")
    public void BuscaDispositivos(@PathVariable String mac) throws Exception {
        endepoint.findDeviceByMacAddress(mac);
    }


}

