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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SheduledService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private RepositoryEntity entityRepository;
    @Autowired
    EndpointService endpointService;
    private final String os = System.getProperty("os.name").toLowerCase();
    private final ExecutorService executorService = Executors.newFixedThreadPool(50);

    public SheduledService(RepositoryEntity repository){
        this.entityRepository = repository;
    }

    private final AtomicInteger totalEndpoints = new AtomicInteger(0);
    private final AtomicInteger linhasProcessadas = new AtomicInteger(0);

    @PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
    }

    @Transactional
    @Scheduled(cron = "*/20 * * * * *")
    public void gerar() throws IOException, InterruptedException {
        var endpoints = entityRepository.findAll();
        totalEndpoints.set(endpoints.size());
        linhasProcessadas.set(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (var endpoint : endpoints) {
            futures.add(rodar(endpoint));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Transactional
    public void processarEndpoint(EndpointEntity endpoint) throws IOException, InterruptedException {
        // Lógica de banco de dados que precisa de transação
        PingResult pingResult = executarPing(endpoint.getIp());

        String statusAnterior = endpoint.getStatus();
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime ultimaVezOnline = endpoint.getUltimaVezOnline();

        boolean agoraIntermitente = false;

        if ("UP".equals(pingResult.status)) {
            if (pingResult.latenciaMedia != null && pingResult.latenciaMedia > 150) {
                agoraIntermitente = true;
            } else if ("DOWN".equals(statusAnterior) && ultimaVezOnline != null) {
                long segundosOffline = Duration.between(ultimaVezOnline, agora).getSeconds();
                if (segundosOffline <= 60) {
                    agoraIntermitente = true;
                }
            }
        }

        // Atualizar o status no banco de dados
        endpoint.setStatus(pingResult.status);
        endpoint.setUltimaVezOnline(agora);
        entityRepository.save(endpoint);
    }

    public CompletableFuture<Void> rodar(EndpointEntity endpoint) {
        return CompletableFuture.runAsync(() -> {
            try {
                processarEndpoint(endpoint);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Executa um único ping -c 4 e extrai tudo de uma vez:
     * - status (UP/DOWN)
     * - detalhes de cada ICMP (para exibir no card)
     * - latência média (para verificar intermitência)
     *
     * A latência salva no banco é a MESMA usada para validar intermitência.
     */
    private PingResult executarPing(String ip) throws IOException, InterruptedException {
        String pingCmd = os.contains("win") ? "ping -n 4 " + ip : "ping -c 4 " + ip;
        Process process = Runtime.getRuntime().exec(pingCmd);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        StringBuilder detalhes = new StringBuilder();
        Pattern icmpPattern = Pattern.compile("icmp_seq=(\\d+) ttl=(\\d+) time=([\\d.]+) ms");
        boolean temResposta = false;
        Double latenciaMedia = null;
        String linha;

        while ((linha = reader.readLine()) != null) {
            // Extrai detalhes de cada pacote ICMP
            Matcher matcher = icmpPattern.matcher(linha);
            if (matcher.find()) {
                temResposta = true;
                String icmpSeq = matcher.group(1);
                String ttl = matcher.group(2);
                String time = matcher.group(3);
                detalhes.append("ICMP Seq: ").append(icmpSeq)
                        .append(", TTL: ").append(ttl)
                        .append(", Tempo: ").append(time).append(" ms\n");
            }

            // Extrai latência média da linha de resumo
            if (linha.contains("rtt min/avg/max")) {
                String[] partes = linha.split("=");
                if (partes.length > 1) {
                    String[] valores = partes[1].trim().split("/");
                    latenciaMedia = Double.parseDouble(valores[1]);
                }
            }
        }

        reader.close();
        process.waitFor();

        String status = temResposta ? "UP" : "DOWN";
        return new PingResult(status, detalhes.toString().trim(), latenciaMedia);
    }

    private record PingResult(String status, String detalhes, Double latenciaMedia) {}

    private void enviarSocket(String mensagem){
        messagingTemplate.convertAndSend("/topic/processamento", mensagem);
    }

    @Scheduled(cron = "*/10 * * * * *")
    public void getVierListPage(){
        var lista = endpointService.getLista().stream().toList();
        messagingTemplate.convertAndSend("/lista", lista);
    }
}
