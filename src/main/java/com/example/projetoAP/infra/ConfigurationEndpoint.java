package com.example.projetoAP.infra;

import com.example.projetoAP.controler.ControlerEndpoint;
import com.example.projetoAP.controler.SetorControler.SetorControler;
import com.example.projetoAP.controler.SetoresService.SetorService;
import com.example.projetoAP.controler.service.*;
import com.example.projetoAP.repository.EntitySetor.FactureSetor;
import com.example.projetoAP.repository.EntitySetor.RepositorySetor;
import com.example.projetoAP.repository.ICMPRepository;
import com.example.projetoAP.repository.MetricasRepository;
import com.example.projetoAP.repository.RepositoryEntity;
import com.example.projetoAP.repository.SalvaEndPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

@Configuration
public class ConfigurationEndpoint {
   @Bean
   public RepositoryEntity repository(RepositoryEntity repository){
       return repository;
   }
   @Bean
   public ICMPRepository icmpRepository(ICMPRepository icmpRepository){
       return icmpRepository;
   }

    @Bean
    public ControlerEndpoint controlerEndpoint(EndepointBuscadosService endepointBuscadosService) {
        return new ControlerEndpoint(endepointBuscadosService);
    }
    @Bean
     public ItensMetricasService ItensMetricasService(ICMPRepository icmpRepository){
        return  new ItensMetricasService(icmpRepository);
    }
    @Bean
    public EndpointService endpointServicen(EndpointService e){
        return e;
    }
    @Bean
    public SalvaEndPoint GetControler(SalvaEndPoint salvaEndPoint){
        return salvaEndPoint;
    }

    @Bean
    public SnmpService snmp(){
       return new SnmpService();
    }
    @Bean
    public EndepointBuscadosService endepointBuscadosService(RepositoryEntity repository,SnmpService snmp) {
        return new EndepointBuscadosService(repository,snmp);
    }
//    @Bean
//    public RepositorySetor repositorySetor(RepositorySetor repository){
//       return repository;
//    }
    @Bean
    public FactureSetor factureSetor(RepositorySetor repository){
       return new FactureSetor(repository);

    }
    @Bean
    public SetorService setorService(FactureSetor factureSetor){
       return new SetorService(factureSetor);
    }









}
