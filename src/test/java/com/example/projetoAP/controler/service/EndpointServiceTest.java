package com.example.projetoAP.controler.service;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.domain.model.Endpoin;
import com.example.projetoAP.repository.SalvaEndPoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class EndpointServiceTest {
    @Test
   public void shouldSaveEndpoint() throws IOException, InterruptedException {
       EndpointDTO endpointDTO = new EndpointDTO("app","nome","sss","jjsdfjsd","10.88.1","on","sdsdf","sfsd","sdfsd","1",1,"sw");
       Endpoin s = new Endpoin(endpointDTO);
       Assertions.assertEquals(endpointDTO.ReceiveGrupoId(),s.getGrupoId());

   }
}