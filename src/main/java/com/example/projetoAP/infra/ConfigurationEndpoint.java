package com.example.projetoAP.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.projetoAP.controler.ControlerEndpoint;
import com.example.projetoAP.controler.service.EndepointBuscadosService;
import com.example.projetoAP.controler.service.EndpointService;
import com.example.projetoAP.controler.service.ItensMetricasService;
import com.example.projetoAP.controler.service.SnmpService;
import com.example.projetoAP.repository.ICMPRepository;
import com.example.projetoAP.repository.RepositoryEntity;
import com.example.projetoAP.repository.SalvaEndPoint;

@Configuration
public class ConfigurationEndpoint {
    private final String token = "7938730712:AAEJur7PV7JgRC4c9dlQ49310p4L9NkSbFM";
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
    // @Bean
    // public BotService botService() throws TelegramApiException {
    //     BotService botService = new BotService("Elivandro_bot",token);
    //    var telegram = new TelegramBotsApi(DefaultBotSession.class);
    //    telegram.registerBot(botService);
    //    return botService;
    // }









}
