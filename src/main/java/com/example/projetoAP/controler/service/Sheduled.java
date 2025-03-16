package com.example.projetoAP.controler.service;

import com.example.projetoAP.repository.EndpointEntity;
import com.example.projetoAP.repository.RepositoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

@Component
public class Sheduled {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private RepositoryEntity entityRepository;
    // Defina um ThreadPoolExecutor com controle de número de threads
    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // Aqui você limita a 10 threads simultâneas.

    public Sheduled(RepositoryEntity repository){
        this.entityRepository = repository;
    }
    private  int process = 0;
    private  int linhas = 0;
    private boolean isProcess = false;
    double processo = (double) linhas / process * 100;

    public CompletableFuture<String> statusIpAsync(String host) throws IOException {
        InetAddress inet = InetAddress.getByName(host);
        boolean isReactive = inet.isReachable(3000);
        return CompletableFuture.completedFuture(isReactive ? "UP" : "DOWN");
    }

    @Transactional
    @Scheduled(cron = "* * * * * *")
    public void gerar() throws IOException, InterruptedException {
        var endpoints = entityRepository.findAll();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Aqui você itera sobre os endpoints e cria uma tarefa para cada um deles
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
                if(process<linhas){
                    linhas = 0;
                }
                String mensagem = String.format("Processando... %d/%d (%.2f%%)",linhas, process,processo);
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
    }
}
