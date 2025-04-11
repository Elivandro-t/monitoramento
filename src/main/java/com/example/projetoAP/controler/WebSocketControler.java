package com.example.projetoAP.controler;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.controler.service.EndpointService;
import com.example.projetoAP.controler.service.GroupService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;

@Controller
public class WebSocketControler {
//    GroupService groupService;
//    EndpointService endpointService;
//    WebSocketControler(EndpointService endpointService){
//        this.endpointService = endpointService;
//    }
//
//    @MessageMapping("/sendData")
//    @SendTo("/lista")
////    public List<EndpointDTO> getVierListPage(){
////            // Filtra a lista baseado no MAC
////            var lista = endpointService.getLista().stream().toList();
////            return lista;
////
////    }
}
