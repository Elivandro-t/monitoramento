package com.example.projetoAP.controler.service;

import com.example.projetoAP.repository.EndpointEntity;
import com.example.projetoAP.repository.RepositoryEntity;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SheduledService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private RepositoryEntity entityRepository;
    @Autowired
    EndpointService endpointService;
    // Defina um ThreadPoolExecutor com controle de número de threads
    private final ExecutorService executorService = Executors.newFixedThreadPool(50); // Aqui você limita a 10 threads simultâneas.
    public SheduledService(RepositoryEntity repository){
        this.entityRepository = repository;
    }
    private  int process = 0;
    private  int linhas = 0;
    public CompletableFuture<String> statusIpAsync(String host) throws IOException {
        InetAddress inet = InetAddress.getByName(host);
        boolean isReactive = inet.isReachable(3000);
        return CompletableFuture.completedFuture(isReactive ? "UP" : "DOWN");
    }
    @PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
    }
    @Transactional
    @Scheduled(cron = "*/20 * * * * *")
    public void gerar() throws IOException, InterruptedException {
        var endpoints = entityRepository.findAll();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (var endpoint : endpoints) {
            futures.add(rodar(endpoint));
            process = endpoints.size();
            if(endpoints.size() < process){
                process =  endpoints.size();
            }
        }
        // Espera todas as tarefas assíncronas terminarem
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
    // A execução assíncrona agora está usando o ExecutorService
    public CompletableFuture<Void> rodar(EndpointEntity endpoint) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Verifica o status e atualiza o endpoint
                String status = statusIpAsync(endpoint.getIp()).get(); // Espera o status para cada IP
                String ping = getCmd(endpoint.getIp());
                String statusOn = statusIp(endpoint.getIp());
                LocalDateTime dataUltimaVez = endpoint.getUltimaVezOnline();;
                if(endpoint.getStatus().equals("UP")){
                    dataUltimaVez = LocalDateTime.now();
                }
                endpoint.setStatus(status);

                endpointService.atualizarStatusParcial(endpoint.getId(), statusOn, ping,dataUltimaVez);
                linhas++;
                double processo = Math.abs((double) linhas / process * 100);
                if(process<linhas){
                    linhas = 0;
                }
                String mensagem;
                if(linhas == process){
                     mensagem = String.format("Processado %d/%d ( %.0f%% )",linhas, process,processo);

                }else{
                    mensagem = String.format("Processando... %d/%d ( %.0f%% )",linhas, process,processo);
                }
                EnvidoSoquet(mensagem);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executorService);  // Usando o executorService para controlar o número de threads
    }
    // Shutdown do executorService após o processamento
    public void shutdown() {
        try {
            if (executorService != null) {
                executorService.shutdown();
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
    private void EnvidoSoquet(String mensagem){

        messagingTemplate.convertAndSend("/topic/processamento", mensagem );
//        messagingTemplate.convertAndSend("/off", lista.stream().filter(e->e.status().equals("DOWN")).toList() );
    }
    @Scheduled(cron = "*/10 * * * * *")
    public void getVierListPage(){
        // Filtra a lista baseado no MAC
        var lista = endpointService.getLista().stream().toList();
        messagingTemplate.convertAndSend("/lista", lista);
    }
    public String getCmd(String  host) throws IOException, InterruptedException {
        String linha = null;
        String os = System.getProperty("os.name").toLowerCase();
        String pingCmd = os.contains("win") ? "ping -n 4 " + host : "ping -c 4 " + host;
        Process process = Runtime.getRuntime().exec(pingCmd);
        StringBuilder result = new StringBuilder();
        String line;
        Pattern pattern = Pattern.compile("icmp_seq=(\\d+) ttl=(\\d+) time=([\\d.]+) ms");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while ((line = bufferedReader.readLine())!=null){
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String icmpSeq = matcher.group(1);
                String ttl = matcher.group(2);
                String time = matcher.group(3);
                result.append("ICMP Seq: ").append(icmpSeq)
                        .append(", TTL: ").append(ttl)
                        .append(", Tempo: ").append(time).append(" ms\n");
            }
        }
        process.waitFor();
        return result.toString().trim();
    }
    public String statusIp(String host) throws IOException {
//        InetAddress inet = InetAddress.getByName(host);
//        boolean isReactive = inet.isReachable(300);
        Process p = Runtime.getRuntime().exec("ping -c 1 " + host); // No Windows, use "ping -n 1"
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        boolean isReactive = false;
        while ((line = input.readLine()) != null) {
            if (line.contains("ttl=") || line.contains("TTL=")) {
                isReactive = true;
                break;
            }
        }
        input.close();
        return isReactive ? "UP":"DOWN";
    }
}
