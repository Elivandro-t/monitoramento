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

}
