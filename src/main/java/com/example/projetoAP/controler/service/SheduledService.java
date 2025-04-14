package com.example.projetoAP.controler.service;

import com.example.projetoAP.repository.EndpointEntity;
import com.example.projetoAP.repository.RepositoryEntity;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

@Component
public class SheduledService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private RepositoryEntity entityRepository;
    @Autowired
    EndpointService endpointService;

    // Defina um ThreadPoolExecutor com controle de número de threads
    private final ExecutorService executorService = Executors.newFixedThreadPool(30); // Aqui você limita a 10 threads simultâneas.

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
                endpoint.setStatus(status);
                endpoint.atualiza(endpoint.getIp()); // Atualiza o ping também
                entityRepository.save(endpoint);
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

//    @Scheduled(cron = "* * * * * *")
//    public void getVierListPage(){
//        // Filtra a lista baseado no MAC
//        var lista = endpointService.getLista().stream().toList();
//        messagingTemplate.convertAndSend("/lista", lista);
//
//    }
}
