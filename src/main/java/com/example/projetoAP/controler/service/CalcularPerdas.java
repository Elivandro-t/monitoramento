package com.example.projetoAP.controler.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.projetoAP.domain.model.Metricas;
import com.example.projetoAP.domain.vo.Filter;
import com.example.projetoAP.domain.vo.FilterHost;
import com.example.projetoAP.domain.vo.LostDTO;
import com.example.projetoAP.domain.vo.MetricasDTO;
import com.example.projetoAP.repository.ICMPRepository;
import com.example.projetoAP.repository.ItemsICMPEntity;
import com.example.projetoAP.repository.MetricasEntity;
import com.example.projetoAP.repository.MetricasRepository;
import com.example.projetoAP.repository.RepositoryEntity;

import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;

@Service
public class CalcularPerdas {
    private RepositoryEntity repository;
    private ICMPRepository icmpRepository;
    private MetricasRepository metricasRepository;
    private final String os = System.getProperty("os.name").toLowerCase();
    ExecutorService executor = Executors.newCachedThreadPool();
    private static final long CACHE_TTL_MILLIS = 1000;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static final class CacheEntry {
        final Map<String, List<MetricasDTO>> result;
        final long timestamp;

        private CacheEntry(Map<String, List<MetricasDTO>> result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        executor.shutdown();
    }

    public CalcularPerdas(RepositoryEntity repository,
                          ICMPRepository icmpRepository,
                          MetricasRepository metricasRepository) {
        this.repository = repository;
        this.icmpRepository = icmpRepository;
        this.metricasRepository = metricasRepository;
    }

    @Scheduled(fixedDelay = 1000)
    public void CalcularMestricasDeEquipamento() {
        var equipamento = repository.findAll();
        var icmp = icmpRepository.findAll();
        List<CompletableFuture<Void>> lista = new ArrayList<>();
        if (equipamento.isEmpty()) {
            return;
        }
        for (var equipamentos : equipamento) {
            lista.add(runFuntion(equipamentos.getIp(), icmp, equipamentos.getId()));
        }
        CompletableFuture.allOf(lista.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Resultado de um ping -c 4 com todas as metricas extraidas.
     */
    private record PingStats(
            double latenciaMedia,
            double latenciaMin,
            double latenciaMax,
            double jitter,
            int perdaPercent
    ) {}

    /**
     * Executa um unico ping -c 4 e extrai tudo:
     * - Perda de pacotes (%)
     * - Latencia min/avg/max/jitter (ms)
     */
    private PingStats executarPingCompleto(String ip) throws IOException, InterruptedException {
        String pingCmd = os.contains("win") ? "ping -n 4 " + ip : "ping -c 4 " + ip;
        Process process = Runtime.getRuntime().exec(pingCmd);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        int pacotesRecebidos = 0;
        int pacotesEnviados = 4;
        double min = 0, avg = 0, max = 0, mdev = 0;
        String linha;

        while ((linha = reader.readLine()) != null) {
            if (linha.contains("time=")) {
                pacotesRecebidos++;
            }
            // rtt min/avg/max/mdev = 0.123/0.456/0.789/0.111 ms
            if (linha.contains("rtt min/avg/max")) {
                String[] partes = linha.split("=");
                if (partes.length > 1) {
                    String[] valores = partes[1].trim().split("/");
                    if (valores.length >= 4) {
                        min = Double.parseDouble(valores[0]);
                        avg = Double.parseDouble(valores[1]);
                        max = Double.parseDouble(valores[2]);
                        mdev = Double.parseDouble(valores[3].replace(" ms", "").trim());
                    }
                }
            }
        }

        reader.close();
        process.waitFor();

        int perda = pacotesEnviados > 0
                ? ((pacotesEnviados - pacotesRecebidos) * 100) / pacotesEnviados
                : 100;

        return new PingStats(avg, min, max, mdev, perda);
    }

    public CompletableFuture<Void> runFuntion(String ip, List<ItemsICMPEntity> icmp, long hostid) {
        return CompletableFuture.runAsync(() -> {
            try {
                PingStats stats = executarPingCompleto(ip);
                List<MetricasEntity> batch = new ArrayList<>();

                for (var item : icmp) {
                    if (item.getKey_().equals("icmppingsec")) {
                        MetricasDTO m = new MetricasDTO(
                                String.valueOf(stats.latenciaMedia()),
                                item.getItemid(), hostid, item.getName(), 0L, "");
                        batch.add(new MetricasEntity(new Metricas(m)));
                    } else if (item.getKey_().equals("icmppingloss")) {
                        LostDTO m = new LostDTO(
                                String.valueOf(stats.perdaPercent()),
                                item.getItemid(), hostid, item.getName());
                        batch.add(new MetricasEntity(new Metricas(m)));
                    }
                }

                // Metricas extras: min, max, jitter (salva direto sem depender da tabela icmp)
                batch.add(new MetricasEntity(new Metricas(
                        new MetricasDTO(String.valueOf(stats.latenciaMin()), 0, hostid, "ICMP min latency", 0L, ""))));
                batch.add(new MetricasEntity(new Metricas(
                        new MetricasDTO(String.valueOf(stats.latenciaMax()), 0, hostid, "ICMP max latency", 0L, ""))));
                batch.add(new MetricasEntity(new Metricas(
                        new MetricasDTO(String.valueOf(stats.jitter()), 0, hostid, "ICMP jitter", 0L, ""))));

                if (!batch.isEmpty()) {
                    metricasRepository.saveAll(batch);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Map<String, List<MetricasDTO>>> listaMetricas(Filter filter) {
        Map<String, List<MetricasDTO>> listaMap = new HashMap<>();
        var lista = metricasRepository.findTop2000ByItemidAndHostidOrderByIdDesc(filter.itemids(), filter.hostid())
                .stream().map(MetricasDTO::new).toList();
        listaMap.put("result", lista);
        return CompletableFuture.supplyAsync(() -> listaMap, executor);
    }

    public Map<String, List<MetricasDTO>> ListaMestricasSemID(FilterHost filter) {
        int hours = filter.hours() == null ? 0 : filter.hours();
        int seconds = filter.seconds() == null ? 0 : filter.seconds();
        String cacheKey = filter.hostid() + ":" + hours + ":" + seconds;
        long now = System.currentTimeMillis();
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && now - cached.timestamp <= CACHE_TTL_MILLIS) {
            return cached.result;
        }

        Map<String, List<MetricasDTO>> listaMap = new HashMap<>();
        List<MetricasEntity> resultado;
        if (seconds > 0) {
            LocalDateTime desde = LocalDateTime.now().minusSeconds(seconds);
            resultado = metricasRepository.findTop5000ByHostidAndTimestampAfterOrderByTimestampDesc(filter.hostid(), desde);
        } else if (hours > 0) {
            LocalDateTime desde = LocalDateTime.now().minusHours(hours);
            resultado = metricasRepository.findTop5000ByHostidAndTimestampAfterOrderByTimestampDesc(filter.hostid(), desde);
        } else {
            resultado = metricasRepository.findTop5000ByHostidOrderByTimestampDesc(filter.hostid());
        }
        var lista = resultado.stream().map(MetricasDTO::new).toList();
        listaMap.put("result", lista);
        cache.put(cacheKey, new CacheEntry(listaMap, now));
        return listaMap;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deletedByTimestamp() {
        LocalDateTime timeDay = LocalDateTime.now().minus(2, ChronoUnit.DAYS);
        var listaBuscada = metricasRepository.findAllByTimestampBefore(timeDay);
        if (!listaBuscada.isEmpty()) {
            metricasRepository.deleteAll(listaBuscada);
        }
    }

    private final Map<Long, LocalDateTime> notfication = new ConcurrentHashMap<>();
}
