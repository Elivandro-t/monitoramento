package com.example.projetoAP.controler;
import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Service.MainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping
public class ControlerEndpoint {
    @Autowired
    private MainService macConectados;

    List<EndpointDTO> listas;
    public ControlerEndpoint(){
        this.listas = new ArrayList<>();
    }

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
}
