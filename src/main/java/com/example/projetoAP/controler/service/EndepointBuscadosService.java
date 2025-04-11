package com.example.projetoAP.controler.service;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.repository.RepositoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class EndepointBuscadosService {
     private SnmpService snmpService;
     private RepositoryEntity repositorya;
     public  EndepointBuscadosService(RepositoryEntity repository,SnmpService snmpService){
         this.repositorya =  repository;
         this.snmpService = snmpService;
     };
     EndepointBuscadosService(){}

    public void findDeviceByMacAddress(String macAddressToSearch) throws Exception {
        // Buscar todos os APs no banco de dados
        List<EndpointDTO> aps = repositorya.findAll().stream().map(EndpointDTO::new).toList();
        System.out.println("Meu mac vindo do front "+macAddressToSearch);

        // Criar uma lista de tarefas assíncronas
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // Para cada AP, criar uma tarefa assíncrona
        for (EndpointDTO ap : aps) {
            futures.add(snmpService.snmp(ap, macAddressToSearch));
        }

        // Esperar todas as tarefas terminarem
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        // Processar os resultados
        for (int i = 0; i < futures.size(); i++) {
            System.out.println("Dados vindo "+futures.get(i).get());
            if (futures.get(i).get()) {
                EndpointDTO ap = aps.get(i);
                System.out.println("Dispositivo " + macAddressToSearch + " encontrado em:");
                System.out.println("Cidade: " + ap.localizacao());
                System.out.println("Rua: " + ap.nome());
                System.out.println("Modelo: " + ap.modelo());
                break;
            }else{
                System.out.println("Nenhum dispositivo encontrado na rede");
            }
        }
    }
}
