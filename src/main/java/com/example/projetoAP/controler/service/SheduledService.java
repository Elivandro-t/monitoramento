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
import java.time.Duration;
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
    private final String os = System.getProperty("os.name").toLowerCase();
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
    public CompletableFuture<Void> rodars(EndpointEntity endpoint) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 1. Armazena o status que está no banco agora (antes de atualizar)
                String statusAnterior = endpoint.getStatus();

                // 2. Executa as verificações (mantendo seus métodos)
                String status = statusIpAsync(endpoint.getIp()).get();
                String ping = getCmd(endpoint.getIp());
                String statusOn = statusIp(endpoint.getIp()); // O status em tempo real

                // 3. Lógica de Intermitência:
                // Se o status no banco era diferente do que acabamos de ler, houve uma mudança.
                boolean agoraIntermitente = false;
                if (statusAnterior != null && !statusAnterior.equals(statusOn)) {
                    agoraIntermitente = true;
                }

                LocalDateTime dataUltimaVez = endpoint.getUltimaVezOnline();
                if(statusOn.equals("UP")){
                    dataUltimaVez = LocalDateTime.now();
                }
                // 4. Atualiza o objeto com os novos valores
                endpoint.setStatus(statusOn);
                endpoint.setIntermitente(agoraIntermitente); // Define se oscilou

                // 5. Envia para o service salvar (Certifique-se de adicionar o novo parâmetro no seu Service)
                endpointService.atualizarStatusParcial(endpoint.getId(), statusOn, ping, dataUltimaVez, agoraIntermitente);

                // --- Restante da sua lógica de progresso e Socket (Mantida) ---
                linhas++;
                double processo = Math.abs((double) linhas / process * 100);
                if(process < linhas) { linhas = 0; }

                String mensagem = (linhas == process)
                        ? String.format("Processado %d/%d ( %.0f%% )", linhas, process, processo)
                        : String.format("Processando... %d/%d ( %.0f%% )", linhas, process, processo);

                EnvidoSoquet(mensagem);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executorService);
    }
    public CompletableFuture<Void> rodar(EndpointEntity endpoint) {
        return CompletableFuture.runAsync(() -> {
            try {
                Double latenciaMedia = CalcularLatencia(endpoint.getIp()).get();
                String statusOn = statusIp(endpoint.getIp());
                String pingDetalhado = getCmd(endpoint.getIp());

                String statusAnterior = endpoint.getStatus();
                LocalDateTime agora = LocalDateTime.now();
                LocalDateTime ultimaVezOnline = endpoint.getUltimaVezOnline();

                boolean agoraIntermitente = false;

                if ("UP".equals(statusOn)) {

                    if (latenciaMedia != null && latenciaMedia > 0.150) {
                        agoraIntermitente = true;
                    }
                    else if ("DOWN".equals(statusAnterior) && ultimaVezOnline != null) {
                        long segundosOffline = Duration.between(ultimaVezOnline, agora).getSeconds();
                        if (segundosOffline <= 10) {
                            agoraIntermitente = true;
                        }
                    }

                    ultimaVezOnline = agora;

                } else {

                    if ("UP".equals(statusAnterior) && ultimaVezOnline != null) {
                        long segundosOffline = Duration.between(ultimaVezOnline, agora).getSeconds();
                        if (segundosOffline <= 10) {
                            agoraIntermitente = true;
                        }
                    }
                }

                endpoint.setStatus(statusOn);
                endpoint.setUltimaVezOnline(ultimaVezOnline);

                endpointService.atualizarStatusParcial(
                        endpoint.getId(),
                        statusOn,
                        pingDetalhado,
                        ultimaVezOnline,
                        agoraIntermitente
                );

                if ("DOWN".equals(statusOn) && ultimaVezOnline != null) {
                    long segundosOffline = Duration.between(ultimaVezOnline, agora).getSeconds();
                    System.out.println("IP: " + endpoint.getIp() + " offline há " + segundosOffline + " segundos");
                }

                linhas++;
                double porcentagem = Math.abs((double) linhas / process * 100);

                String mensagem = (linhas >= process)
                        ? String.format("Processado %d/%d ( %.0f%% )", linhas, process, porcentagem)
                        : String.format("Processando... %d/%d ( %.0f%% )", linhas, process, porcentagem);

                EnvidoSoquet(mensagem);

            } catch (Exception e) {
                System.out.println("Erro no endpoint: " + endpoint.getIp());
                e.printStackTrace();
            }
        }, executorService);
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
                double timeMs = Double.parseDouble(matcher.group(3)); // em ms
                double timeSec = timeMs / 1000.0; // converter para segundos
                result.append("ICMP Seq: ").append(icmpSeq)
                        .append(", TTL: ").append(ttl)
                        .append(", Tempo: ").append(timeSec).append("nao seria melhor retornar sem 0s\n");
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
}
