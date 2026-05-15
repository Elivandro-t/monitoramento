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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SheduledService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final RepositoryEntity entityRepository;

    @Autowired
    private EndpointService endpointService;

    private final String os = System.getProperty("os.name").toLowerCase();

    // 🔧 reduzindo threads para evitar sobrecarga
    private final ExecutorService executorService = Executors.newFixedThreadPool(3); // Reduzido de 5 para 3

    // 🔒 controle para evitar concorrência do scheduler
    private final AtomicBoolean emExecucao = new AtomicBoolean(false);
    
    // Limita processamento paralelo com Semaphore
    private final Semaphore semaphore = new Semaphore(10); // Máximo 10 endpoints simultâneos

    public SheduledService(RepositoryEntity repository) {
        this.entityRepository = repository;
    }

    @PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
    }

    @Scheduled(cron = "0 */2 * * * *")  // A cada 2 minutos em vez de 30 segundos
    public void gerar() {

        // 🚫 evita rodar múltiplas execuções ao mesmo tempo
        if (!emExecucao.compareAndSet(false, true)) {
            System.out.println("⏭️  Scheduler já em execução, pulando ciclo");
            return;
        }

        try {
            var endpoints = entityRepository.findAll();
            
            if (endpoints.isEmpty()) {
                return;
            }

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (var endpoint : endpoints) {
                try {
                    futures.add(rodar(endpoint));
                } catch (Exception e) {
                    System.err.println("❌ Erro ao agendar processamento do endpoint " + endpoint.getId() + ": " + e.getMessage());
                }
            }

            // Aguarda com timeout para não travar indefinidamente
            try {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .orTimeout(120, TimeUnit.SECONDS)  // Timeout de 2 minutos
                    .join();
            } catch (CompletionException e) {
                System.err.println("⏱️  Timeout ou erro ao processar endpoints: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("❌ Erro crítico no scheduler: " + e.getMessage());
            e.printStackTrace();
        } finally {
            emExecucao.set(false);
        }
    }

    public CompletableFuture<Void> rodar(EndpointEntity endpoint) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Adquire permissão do semáforo (máximo 10 simultâneos)
                semaphore.acquire();
                try {
                    processarEndpoint(endpoint.getId());
                } finally {
                    semaphore.release();
                }
            } catch (InterruptedException e) {
                System.err.println("⚠️  Thread interrompida ao processar endpoint " + endpoint.getId());
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("❌ Erro ao processar endpoint " + endpoint.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, executorService);
    }

    /**
     * ✅ Cada thread busca a entidade novamente no banco
     * ✅ Agora com transação correta
     */
    @Transactional
    public void processarEndpoint(Long id) throws IOException, InterruptedException {

        EndpointEntity endpoint = entityRepository.findById(id).orElseThrow();

        PingResult pingResult = executarPing(endpoint.getIp());

        String statusAnterior = endpoint.getStatus();
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime ultimaVezOnline = endpoint.getUltimaVezOnline();

        boolean agoraIntermitente = false;

        if ("UP".equals(pingResult.status())) {
            if (pingResult.latenciaMedia() != null && pingResult.latenciaMedia() > 150) {
                agoraIntermitente = true;
            } else if ("DOWN".equals(statusAnterior) && ultimaVezOnline != null) {
                long segundosOffline = Duration.between(ultimaVezOnline, agora).getSeconds();
                if (segundosOffline <= 60) {
                    agoraIntermitente = true;
                }
            }
        }

        endpoint.setStatus(pingResult.status());
        endpoint.setUltimaVezOnline(agora);

        entityRepository.save(endpoint);
    }

    private PingResult executarPing(String ip) throws IOException, InterruptedException {
        String pingCmd = os.contains("win") ? "ping -n 4 " + ip : "ping -c 4 " + ip;

        Process process = Runtime.getRuntime().exec(pingCmd);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        StringBuilder detalhes = new StringBuilder();
        Pattern icmpPattern = Pattern.compile("icmp_seq=(\\d+) ttl=(\\d+) time=([\\d.]+) ms");

        boolean temResposta = false;
        Double latenciaMedia = null;

        String linha;

        while ((linha = reader.readLine()) != null) {

            Matcher matcher = icmpPattern.matcher(linha);

            if (matcher.find()) {
                temResposta = true;

                detalhes.append("ICMP Seq: ").append(matcher.group(1))
                        .append(", TTL: ").append(matcher.group(2))
                        .append(", Tempo: ").append(matcher.group(3))
                        .append(" ms\n");
            }

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

    private void enviarSocket(String mensagem) {
        messagingTemplate.convertAndSend("/topic/processamento", mensagem);
    }

    @Scheduled(cron = "*/30 * * * * *")
    public void getVierListPage() {
        var lista = endpointService.getLista();
        messagingTemplate.convertAndSend("/lista", lista);
    }
}