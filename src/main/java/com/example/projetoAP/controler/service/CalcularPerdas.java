package com.example.projetoAP.controler.service;

import com.example.projetoAP.domain.model.Metricas;
import com.example.projetoAP.domain.vo.LostDTO;
import com.example.projetoAP.domain.vo.MetricasDTO;
import com.example.projetoAP.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class CalcularPerdas {
     private RepositoryEntity repository;
     private ICMPRepository icmpRepository;
     private MetricasRepository metricasRepository;
   private final String os = System.getProperty("os.name").toLowerCase();
    //Código omitido

    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    //Código omitido
    public CalcularPerdas(RepositoryEntity repository,ICMPRepository icmpRepository,MetricasRepository metricasRepository){

        this.repository = repository;
        this.icmpRepository = icmpRepository;
        this.metricasRepository = metricasRepository;
    }
    @Scheduled(cron = "0 * * * * *")
    public void CalcularMestricasDeEquipamento(){
        var equipamento = repository.findAll();
        var icmp = icmpRepository.findAll();
        List<CompletableFuture<Void>> lista = new ArrayList<>();
        if(equipamento.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipamento não encontrado");

        }
        for (var equipamentos:equipamento) {
            String ip = equipamentos.getIp();
            lista.add(runFuntion(equipamentos.getIp(),icmp,equipamentos.getId()));
            long time = calculaTempo(equipamentos);
        }

        CompletableFuture.allOf(lista.toArray(new CompletableFuture[0])).join();
    }



    private CompletableFuture<String> calcularPerdas(String ip)  {
       try {
           String pingCmd = os.contains("win") ? "ping -n 4 " + ip : "ping -c 4 " + ip;

           Process process = Runtime.getRuntime().exec(pingCmd);

           BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
           String linhas;
           int pacotesEnviado = 4, pacotesRecebidos = 0;
           while ((linhas = bufferedReader.readLine())!=null){
               System.out.println("PING: " + linhas);

               if(linhas.contains("time=")) pacotesRecebidos++;
           }
           var resposta  = String.valueOf(((pacotesEnviado - pacotesRecebidos)*100) / pacotesEnviado);
           return CompletableFuture.completedFuture(resposta);
       }catch (IOException e){
           e.printStackTrace();
           return CompletableFuture.completedFuture(String.valueOf(100));
       }
    }
    public CompletableFuture<Map<String,List<MetricasDTO>>> listaMetricas(Filter filter){
        Map<String,List<MetricasDTO>> listaMap = new HashMap<>();
       var lista =  metricasRepository.findAllById(filter.itemids(),filter.hostid()).stream().map(MetricasDTO::new).toList();
       listaMap.put("result",lista);

       return  CompletableFuture.supplyAsync(()->listaMap);

    }
    public CompletableFuture listas(){
        return CompletableFuture.runAsync(()->{});
    }
    private CompletableFuture<Double> CalcularLatencia(String ip) throws IOException {
            String pingCmd = os.contains("win") ? "ping -n 1 " + ip : "ping -c 1 " + ip;

            Process process = Runtime.getRuntime().exec(pingCmd);
            String linha;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            double latenciaMedia = 0;

            while ((linha = bufferedReader.readLine()) != null) {
                if (linha.contains("rtt min/avg/max")) {
                    String[] partes = linha.split("=");
                    if (partes.length > 1) {
                        String[] valores = partes[1].trim().split("/");
                        latenciaMedia = Double.parseDouble(valores[1]);
                    }
                }
            }
        return CompletableFuture.completedFuture(latenciaMedia);

    }

    private long calculaTempo(EndpointEntity endpointDTO){
        LocalDateTime localDateTime = LocalDateTime.now();
        if("UP".equalsIgnoreCase(endpointDTO.getStatus())){
            return 0;
        }
        return Duration.between(endpointDTO.getUltimaVezOnline(),localDateTime).toMinutes();
    }

    public CompletableFuture<Void> runFuntion(String ip,List<ItemsICMPEntity> icmp,long hostid){
        return CompletableFuture.runAsync(()->{
            try {
                double latencia = CalcularLatencia(ip).get();
                String Perda = calcularPerdas(ip).get();
                System.out.print("Minha latencia "+latencia);
                for(int i = 0;i < icmp.size();i++) {
                    if(icmp.get(i).getKey_().equals("icmppingsec")){
                        MetricasDTO m = new MetricasDTO(String.valueOf(latencia),icmp.get(i).getItemid(),hostid,icmp.get(i).getName(),0l);;
                        Metricas metricas = new Metricas(m);
                        metricasRepository.save(new MetricasEntity(metricas));

                    }else if(icmp.get(i).getKey_().equals("icmppingloss")){
                        LostDTO m = new LostDTO(Perda,icmp.get(i).getItemid(),hostid,icmp.get(i).getName());;
                        Metricas metricas = new Metricas(m);
                        metricasRepository.save(new MetricasEntity(metricas));
                    }

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException a) {
                throw new RuntimeException(a);
            } catch (InterruptedException s) {
                throw new RuntimeException(s);
            }
        },executor);
    }
}
