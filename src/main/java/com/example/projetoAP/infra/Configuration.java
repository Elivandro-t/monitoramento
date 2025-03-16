package com.example.projetoAP.infra;

import com.example.projetoAP.Gateways.EntityRepository;
import com.example.projetoAP.controler.ApControler;
import com.example.projetoAP.controler.GroupControler;
import com.example.projetoAP.controler.service.EndpointService;
import com.example.projetoAP.controler.service.GroupService;
import com.example.projetoAP.controler.service.Sheduled;
import com.example.projetoAP.repository.RepositoryEntity;
import com.example.projetoAP.repository.SalvaEndPoint;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class Configuration {
   @Bean
   public RepositoryEntity repository(RepositoryEntity repository){
       return repository;
   }
    @Bean
    public EndpointService endpointServicen(EndpointService e){
        return e;
    }
    @Bean
    public SalvaEndPoint GetControler(SalvaEndPoint salvaEndPoint){
        return salvaEndPoint;
    }


}
