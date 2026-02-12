package com.example.projetoAP.controler.service;

import com.example.projetoAP.domain.model.Endpoint;
import com.example.projetoAP.domain.model.Metricas;
import com.example.projetoAP.domain.vo.Filter;
import com.example.projetoAP.domain.vo.FilterHost;
import com.example.projetoAP.domain.vo.LostDTO;
import com.example.projetoAP.domain.vo.MetricasDTO;
import com.example.projetoAP.infra.BotService;
import com.example.projetoAP.repository.*;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

@Service
public class CalcularPerdas {
    private BotService service;
    private RepositoryEntity repository;
    private ICMPRepository icmpRepository;
    private MetricasRepository metricasRepository;
    private final String os = System.getProperty("os.name").toLowerCase();
    //Código omitido
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    @PreDestroy
    public void shutdownExecutor() {
        executor.shutdown();
    }
    //Código omitido
    public CalcularPerdas(RepositoryEntity repository,
                          ICMPRepository icmpRepository,
                          MetricasRepository metricasRepository,
                          BotService service
    ){
        this.repository = repository;
        this.icmpRepository = icmpRepository;
        this.service = service;
        this.metricasRepository = metricasRepository;
    }
    @Scheduled(fixedDelay = 1000)  // Executa a cada 30 segundos após o término da execução anterior
    public void CalcularMestricasDeEquipamento(){
        var equipamento = repository.findAll();
        var icmp = icmpRepository.findAll();
        List<CompletableFuture<Void>> lista = new ArrayList<>();
        if(equipamento.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipamento não encontrado");
        }
        for (var equipamentos:equipamento) {
            lista.add(runFuntion(equipamentos.getIp(),icmp,equipamentos.getId()));
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
               if(linhas.contains("time=")) pacotesRecebidos++;
           }
           var resposta  = String.valueOf(((pacotesEnviado - pacotesRecebidos)*100) / pacotesEnviado);
           return CompletableFuture.completedFuture(resposta);
       }catch (IOException e){
           return CompletableFuture.completedFuture(String.valueOf(100));
       }
    }
    public CompletableFuture<Map<String,List<MetricasDTO>>> listaMetricas(Filter filter){
        Map<String,List<MetricasDTO>> listaMap = new HashMap<>();
       var lista =  metricasRepository.findAllById(filter.itemids(),filter.hostid()).stream().limit(2000).map(MetricasDTO::new).toList();
       listaMap.put("result",lista);
       return  CompletableFuture.supplyAsync(()->listaMap);
    }
    public Map<String,List<MetricasDTO>> ListaMestricasSemID(FilterHost filter){
        Map<String,List<MetricasDTO>> listaMap = new HashMap<>();
        var lista =  metricasRepository.findAllByIdOne(filter.hostid()).stream().limit(2000).map(MetricasDTO::new).toList();
        listaMap.put("result",lista);
        return listaMap;
    }
    public CompletableFuture listas(){
        return CompletableFuture.runAsync(()->{});
    }
    private CompletableFuture<Double> CalcularLatencia(String ip) throws IOException {
            String pingCmd = os.contains("win") ? "ping -n 1 " + ip : "ping -c 4 " + ip;
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

    public CompletableFuture<Void> runFuntion(String ip,List<ItemsICMPEntity> icmp,long hostid){
        return CompletableFuture.runAsync(()->{
            try {
                double latencia = CalcularLatencia(ip).get();
                String Perda = calcularPerdas(ip).get();
                List<MetricasEntity> batch = new ArrayList<>();
                for(int i = 0;i < icmp.size();i++) {
                    if(icmp.get(i).getKey_().equals("icmppingsec")){
                        MetricasDTO m = new MetricasDTO(String.valueOf(latencia),icmp.get(i).getItemid(),hostid,icmp.get(i).getName(),0l,"");;
                        Metricas metricas = new Metricas(m);
                        batch.add(new MetricasEntity(metricas));
                    }else if(icmp.get(i).getKey_().equals("icmppingloss")){
                        LostDTO m = new LostDTO(Perda,icmp.get(i).getItemid(),hostid,icmp.get(i).getName());;
                        Metricas metricas = new Metricas(m);
                        batch.add(new MetricasEntity(metricas));

                    }

                }
                if (!batch.isEmpty()) {
                    metricasRepository.saveAll(batch);  // Salva todas as métricas de uma vez
                }
            } catch (IOException | InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        },executor);
    }
//   @Scheduled(cron = "0 0 1 */2 * *")
@Scheduled(cron = "0 0 0 * * *")
@Transactional
public void deletedByTimestamp(){
        LocalDateTime timeDay = LocalDateTime.now().minus(2, ChronoUnit.DAYS);
       var listaBuscada = metricasRepository.findAllByTimestampBefore(timeDay);
       if(!listaBuscada.isEmpty()){
           metricasRepository.deleteAll(listaBuscada);
       }
};
    private final Map<Long,LocalDateTime> notfication = new ConcurrentHashMap<>();

    @Scheduled(cron = "*/30 * * * * *")
    public void enviarRespostaTeste(){
        var equipamento = repository.findAllByStatusDown("DOWN");
        if(equipamento==null){
            this.service.notificationTelegramS();
        }
        assert equipamento != null;
        for (EndpointEntity e:equipamento){           Long id = e.getId();
            LocalDateTime ultimaOnline = e.getUltimaVezOnline();
            Duration tempoOffline = Duration.between(ultimaOnline, LocalDateTime.now());
            // Verifica se está offline há mais de 1 minuto e múltiplos de 1 hora
            if(tempoOffline.toMinutes() < 2) continue;
                LocalDateTime ultimaHoraNotificada = notfication.get(id);
                if (ultimaHoraNotificada !=null && Duration.between(ultimaHoraNotificada,LocalDateTime.now()).toMinutes()< 60) continue;
                    try {
                        service.notificationTelegram(new Endpoint(e));
                        notfication.put(id,LocalDateTime.now());
                    } catch (IOException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }

        }

    }
};
