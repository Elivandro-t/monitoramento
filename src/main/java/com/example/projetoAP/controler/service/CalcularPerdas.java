//package com.example.projetoAP.controler.service;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.Arrays;
//
//import org.springframework.stereotype.Service;
//import org.springframework.scheduling.annotation.Scheduled;
//
//import com.example.projetoAP.domain.model.Metricas;
//import com.example.projetoAP.domain.vo.Filter;
//import com.example.projetoAP.domain.vo.FilterHost;
//import com.example.projetoAP.domain.vo.LostDTO;
//import com.example.projetoAP.domain.vo.MetricasDTO;
//import com.example.projetoAP.repository.ICMPRepository;
//import com.example.projetoAP.repository.ItemsICMPEntity;
//import com.example.projetoAP.repository.MetricasEntity;
//import com.example.projetoAP.repository.MetricasRepository;
//import com.example.projetoAP.repository.RepositoryEntity;
//
//import jakarta.annotation.PreDestroy;
//import jakarta.transaction.Transactional;
//
//@Service
//public class CalcularPerdas {
//    private final RepositoryEntity repository;
//    private final MetricasRepository metricasRepository;
//    private final ICMPRepository icmpRepository;
//    private final String os = System.getProperty("os.name").toLowerCase();
//    ExecutorService executor = Executors.newCachedThreadPool();
//    private static final long CACHE_TTL_MILLIS = 1000;
//    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
//
//    private static final class CacheEntry {
//        final Map<String, List<MetricasDTO>> result;
//        final long timestamp;
//
//        private CacheEntry(Map<String, List<MetricasDTO>> result, long timestamp) {
//            this.result = result;
//            this.timestamp = timestamp;
//        }
//    }
//
//    @PreDestroy
//    public void shutdownExecutor() {
//        executor.shutdown();
//    }
//
//    public CalcularPerdas(RepositoryEntity repository,
//                          ICMPRepository icmpRepository,
//                          MetricasRepository metricasRepository) {
//        this.repository = repository;
//        this.icmpRepository = icmpRepository;
//        this.metricasRepository = metricasRepository;
//    }
//
//    @Scheduled(fixedDelay = 1000)
//    public void CalcularMestricasDeEquipamento() {
//        var equipamento = repository.findAll();
//        var icmp = icmpRepository.findAll();
//        List<CompletableFuture<Void>> lista = new ArrayList<>();
//        if (equipamento.isEmpty()) {
//            return;
//        }
//        for (var equipamentos : equipamento) {
//            lista.add(runFuntion(equipamentos.getIp(), icmp, equipamentos.getId()));
//        }
//        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(lista.toArray(new CompletableFuture[0]));
//        combinedFuture.join();
//    }
//
//    /**
//     * Resultado de um ping -c 4 com todas as metricas extraidas.
//     */
//    private record PingStats(
//            double latenciaMedia,
//            double latenciaMin,
//            double latenciaMax,
//            double jitter,
//            int perdaPercent
//    ) {}
//
//    /**
//     * Executa um unico ping -c 4 e extrai tudo:
//     * - Perda de pacotes (%)
//     * - Latencia min/avg/max/jitter (ms)
//     */
//    private PingStats executarPingCompleto(String ip) throws IOException, InterruptedException {
//        String pingCmd = os.contains("win") ? "ping -n 4 " + ip : "ping -c 4 " + ip;
//        Process process = Runtime.getRuntime().exec(pingCmd);
//
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//
//            int pacotesRecebidos = 0;
//            int pacotesEnviados = 4;
//            double min = 0, avg = 0, max = 0, mdev = 0;
//            String linha;
//
//            while ((linha = reader.readLine()) != null) {
//                if (linha.contains("time=")) {
//                    pacotesRecebidos++;
//                }
//                // rtt min/avg/max/mdev = 0.123/0.456/0.789/0.111 ms
//                if (linha.contains("rtt min/avg/max")) {
//                    String[] partes = linha.split("=");
//                    if (partes.length > 1) {
//                        String[] valores = partes[1].trim().split("/");
//                        if (valores.length >= 4) {
//                            try {
//                                min = Double.parseDouble(valores[0].replace(",", ".").replaceAll("[^0-9.]", ""));
//                                avg = Double.parseDouble(valores[1].replace(",", ".").replaceAll("[^0-9.]", ""));
//                                max = Double.parseDouble(valores[2].replace(",", ".").replaceAll("[^0-9.]", ""));
//                                mdev = Double.parseDouble(valores[3].replace(",", ".").replaceAll("[^0-9.]", "").replace(" ms", ""));
//                            } catch (NumberFormatException e) {
//                                throw new IllegalArgumentException("Erro ao processar valores de latência: " + Arrays.toString(valores), e);
//                            }
//                        }
//                    }
//                }
//            }
//
//            reader.close();
//            process.waitFor();
//
//            int perda = pacotesEnviados > 0
//                    ? ((pacotesEnviados - pacotesRecebidos) * 100) / pacotesEnviados
//                    : 100;
//
//            return new PingStats(avg, min, max, mdev, perda);
//        } catch (IOException | InterruptedException e) {
//            throw new RuntimeException("Erro ao executar ping", e);
//        }
//    }
//
//    public CompletableFuture<Void> runFuntion(String ip, List<ItemsICMPEntity> icmp, long hostid) {
//        return CompletableFuture.runAsync(() -> {
//            try {
//                PingStats stats = executarPingCompleto(ip);
//                List<MetricasEntity> batch = new ArrayList<>();
//
//                for (var item : icmp) {
//                    if (item.getKey_().equals("icmppingsec")) {
//                        MetricasDTO m = new MetricasDTO(
//                                String.valueOf(stats.latenciaMedia()),
//                                item.getItemid(), hostid, item.getName(), 0L, "");
//                        batch.add(new MetricasEntity(new Metricas(m)));
//                    } else if (item.getKey_().equals("icmppingloss")) {
//                        LostDTO m = new LostDTO(
//                                String.valueOf(stats.perdaPercent()),
//                                item.getItemid(), hostid, item.getName());
//                        batch.add(new MetricasEntity(new Metricas(m)));
//                    }
//                }
//
//                // Metricas extras: min, max, jitter (salva direto sem depender da tabela icmp)
//                batch.add(new MetricasEntity(new Metricas(
//                        new MetricasDTO(String.valueOf(stats.latenciaMin()), 0, hostid, "ICMP min latency", 0L, ""))));
//                batch.add(new MetricasEntity(new Metricas(
//                        new MetricasDTO(String.valueOf(stats.latenciaMax()), 0, hostid, "ICMP max latency", 0L, ""))));
//                batch.add(new MetricasEntity(new Metricas(
//                        new MetricasDTO(String.valueOf(stats.jitter()), 0, hostid, "ICMP jitter", 0L, ""))));
//
//                if (!batch.isEmpty()) {
//                    metricasRepository.saveAll(batch);
//                }
//            } catch (IOException | InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }, executor);
//    }
//
//    public CompletableFuture<Map<String, List<MetricasDTO>>> listaMetricas(Filter filter) {
//        return CompletableFuture.supplyAsync(() -> {
//            Map<String, List<MetricasDTO>> listaMap = new HashMap<>();
//            var lista = metricasRepository.findTop2000ByItemidAndHostidOrderByIdDesc(filter.itemids(), filter.hostid())
//                    .stream().map(MetricasDTO::new).collect(Collectors.toList());
//            listaMap.put("result", lista);
//            return listaMap;
//        });
//    }
//
//    public Map<String, List<MetricasDTO>> ListaMestricasSemID(FilterHost filter) {
//        int hours = (filter.hours() != null) ? filter.hours() : 0;
//        int seconds = (filter.seconds() != null) ? filter.seconds() : 0;
//        String cacheKey = filter.hostid() + ":" + hours + ":" + seconds;
//        long now = System.currentTimeMillis();
//        CacheEntry cached = cache.get(cacheKey);
//        if (cached != null && now - cached.timestamp <= CACHE_TTL_MILLIS) {
//            return cached.result;
//        }
//
//        Map<String, List<MetricasDTO>> listaMap = new HashMap<>();
//        List<MetricasEntity> resultado;
//        if (seconds > 0) {
//            LocalDateTime desde = LocalDateTime.now().minusSeconds(seconds);
//            resultado = metricasRepository.findTop5000ByHostidAndTimestampAfterOrderByTimestampDesc(filter.hostid(), desde);
//        } else if (hours > 0) {
//            LocalDateTime desde = LocalDateTime.now().minusHours(hours);
//            resultado = metricasRepository.findTop5000ByHostidAndTimestampAfterOrderByTimestampDesc(filter.hostid(), desde);
//        } else {
//            resultado = metricasRepository.findTop5000ByHostidOrderByTimestampDesc(filter.hostid());
//        }
//        var lista = resultado.stream().map(MetricasDTO::new).collect(Collectors.toList());
//        listaMap.put("result", lista);
//        cache.put(cacheKey, new CacheEntry(listaMap, now));
//        return listaMap;
//    }
//
//    @Scheduled(cron = "0 0 0 * * *")
//    @Transactional
//    public void deletedByTimestamp() {
//        LocalDateTime timeDay = LocalDateTime.now().minus(2, ChronoUnit.DAYS);
//        var listaBuscada = metricasRepository.findAllByTimestampBefore(timeDay);
//        if (!listaBuscada.isEmpty()) {
//            metricasRepository.deleteAll(listaBuscada);
//        }
//    }
//}




//
//package com.example.projetoAP.controler.service;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//import java.util.stream.Collectors;
//import java.util.concurrent.*;
//
//import org.springframework.stereotype.Service;
//import org.springframework.scheduling.annotation.Scheduled;
//
//import com.example.projetoAP.domain.model.Metricas;
//import com.example.projetoAP.domain.vo.*;
//import com.example.projetoAP.repository.*;
//
//import jakarta.annotation.PreDestroy;
//import jakarta.transaction.Transactional;
//
//@Service
//public class CalcularPerdas {
//
//    private final RepositoryEntity repository;
//    private final MetricasRepository metricasRepository;
//    private final ICMPRepository icmpRepository;
//
//    private final String os = System.getProperty("os.name").toLowerCase();
//
//    // 🔥 THREAD POOL CONTROLADO
//    private final ExecutorService executor = Executors.newFixedThreadPool(50);
//
//    // 🔥 EVITA EXECUÇÕES SOBREPOSTAS
//    private volatile boolean emExecucao = false;
//
//    private static final long CACHE_TTL_MILLIS = 1000;
//    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
//
//    private static final class CacheEntry {
//        final Map<String, List<MetricasDTO>> result;
//        final long timestamp;
//
//        private CacheEntry(Map<String, List<MetricasDTO>> result, long timestamp) {
//            this.result = result;
//            this.timestamp = timestamp;
//        }
//    }
//
//    public CalcularPerdas(RepositoryEntity repository,
//                          ICMPRepository icmpRepository,
//                          MetricasRepository metricasRepository) {
//        this.repository = repository;
//        this.icmpRepository = icmpRepository;
//        this.metricasRepository = metricasRepository;
//    }
//
//    @PreDestroy
//    public void shutdownExecutor() {
//        executor.shutdown();
//    }
//
//    // ⏱️ CORRIGIDO PARA 20 SEGUNDOS
//    @Scheduled(fixedDelay = 20000)
//    public void CalcularMestricasDeEquipamento() {
//
//        if (emExecucao) {
//            return; // evita empilhar execuções
//        }
//
//        emExecucao = true;
//
//        try {
//            var equipamentos = repository.findAll();
//            var icmp = icmpRepository.findAll();
//
//            if (equipamentos.isEmpty()) return;
//
//            List<CompletableFuture<Void>> futures = new ArrayList<>();
//
//            for (var eq : equipamentos) {
//                futures.add(runFunction(eq.getIp(), icmp, eq.getId()));
//            }
//
//            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//        } finally {
//            emExecucao = false;
//        }
//    }
//
//    private record PingStats(
//            double latenciaMedia,
//            double latenciaMin,
//            double latenciaMax,
//            double jitter,
//            int perdaPercent
//    ) {}
//
//    private PingStats executarPingCompleto(String ip) {
//
//        try {
//            ProcessBuilder pb;
//
//            // 🔥 APENAS 1 PACOTE (muito mais leve)
//            if (os.contains("win")) {
//                pb = new ProcessBuilder("ping", "-n", "1", ip);
//            } else {
//                pb = new ProcessBuilder("ping", "-c", "1", ip);
//            }
//
//            pb.redirectErrorStream(true);
//            Process process = pb.start();
//
//            boolean terminou = process.waitFor(5, TimeUnit.SECONDS);
//
//            if (!terminou) {
//                process.destroyForcibly();
//                return new PingStats(0, 0, 0, 0, 100);
//            }
//
//            BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(process.getInputStream())
//            );
//
//            String linha;
//            double tempo = 0;
//            boolean recebeu = false;
//
//            while ((linha = reader.readLine()) != null) {
//                if (linha.contains("time=")) {
//                    recebeu = true;
//                    try {
//                        String t = linha.split("time=")[1].split(" ")[0]
//                                .replace(",", ".");
//                        tempo = Double.parseDouble(t);
//                    } catch (Exception ignored) {}
//                }
//            }
//
//            int perda = recebeu ? 0 : 100;
//
//            return new PingStats(
//                    tempo,
//                    tempo,
//                    tempo,
//                    0,
//                    perda
//            );
//
//        } catch (Exception e) {
//            return new PingStats(0, 0, 0, 0, 100);
//        }
//    }
//
//    public CompletableFuture<Void> runFunction(String ip, List<ItemsICMPEntity> icmp, long hostid) {
//
//        return CompletableFuture.runAsync(() -> {
//
//            PingStats stats = executarPingCompleto(ip);
//
//            List<MetricasEntity> batch = new ArrayList<>();
//
//            for (var item : icmp) {
//
//                if ("icmppingsec".equals(item.getKey_())) {
//
//                    MetricasDTO m = new MetricasDTO(
//                            String.valueOf(stats.latenciaMedia()),
//                            item.getItemid(), hostid, item.getName(), 0L, ""
//                    );
//
//                    batch.add(new MetricasEntity(new Metricas(m)));
//
//                } else if ("icmppingloss".equals(item.getKey_())) {
//
//                    LostDTO m = new LostDTO(
//                            String.valueOf(stats.perdaPercent()),
//                            item.getItemid(), hostid, item.getName()
//                    );
//
//                    batch.add(new MetricasEntity(new Metricas(m)));
//                }
//            }
//
//            // extras
//            batch.add(new MetricasEntity(new Metricas(
//                    new MetricasDTO(String.valueOf(stats.latenciaMin()), 0, hostid, "ICMP min latency", 0L, ""))));
//
//            batch.add(new MetricasEntity(new Metricas(
//                    new MetricasDTO(String.valueOf(stats.latenciaMax()), 0, hostid, "ICMP max latency", 0L, ""))));
//
//            batch.add(new MetricasEntity(new Metricas(
//                    new MetricasDTO(String.valueOf(stats.jitter()), 0, hostid, "ICMP jitter", 0L, ""))));
//
//            if (!batch.isEmpty()) {
//                metricasRepository.saveAll(batch);
//            }
//
//        }, executor);
//    }
//
//    public CompletableFuture<Map<String, List<MetricasDTO>>> listaMetricas(Filter filter) {
//        return CompletableFuture.supplyAsync(() -> {
//            Map<String, List<MetricasDTO>> listaMap = new HashMap<>();
//
//            var lista = metricasRepository
//                    .findTop2000ByItemidAndHostidOrderByIdDesc(filter.itemids(), filter.hostid())
//                    .stream()
//                    .map(MetricasDTO::new)
//                    .collect(Collectors.toList());
//
//            listaMap.put("result", lista);
//            return listaMap;
//        });
//    }
//
//    public Map<String, List<MetricasDTO>> ListaMestricasSemID(FilterHost filter) {
//
//        int hours = (filter.hours() != null) ? filter.hours() : 0;
//        int seconds = (filter.seconds() != null) ? filter.seconds() : 0;
//
//        String cacheKey = filter.hostid() + ":" + hours + ":" + seconds;
//
//        long now = System.currentTimeMillis();
//        CacheEntry cached = cache.get(cacheKey);
//
//        if (cached != null && now - cached.timestamp <= CACHE_TTL_MILLIS) {
//            return cached.result;
//        }
//
//        List<MetricasEntity> resultado;
//
//        if (seconds > 0) {
//            LocalDateTime desde = LocalDateTime.now().minusSeconds(seconds);
//            resultado = metricasRepository.findTop5000ByHostidAndTimestampAfterOrderByTimestampDesc(filter.hostid(), desde);
//
//        } else if (hours > 0) {
//            LocalDateTime desde = LocalDateTime.now().minusHours(hours);
//            resultado = metricasRepository.findTop5000ByHostidAndTimestampAfterOrderByTimestampDesc(filter.hostid(), desde);
//
//        } else {
//            resultado = metricasRepository.findTop5000ByHostidOrderByTimestampDesc(filter.hostid());
//        }
//
//        var lista = resultado.stream().map(MetricasDTO::new).collect(Collectors.toList());
//
//        Map<String, List<MetricasDTO>> listaMap = new HashMap<>();
//        listaMap.put("result", lista);
//
//        cache.put(cacheKey, new CacheEntry(listaMap, now));
//
//        return listaMap;
//    }
//
//    @Scheduled(cron = "0 0 0 * * *")
//    @Transactional
//    public void deletedByTimestamp() {
//
//        LocalDateTime timeDay = LocalDateTime.now().minus(2, ChronoUnit.DAYS);
//
//        var lista = metricasRepository.findAllByTimestampBefore(timeDay);
//
//        if (!lista.isEmpty()) {
//            metricasRepository.deleteAll(lista);
//        }
//    }
//}


//usando fping

package com.example.projetoAP.controler.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import com.example.projetoAP.domain.model.Metricas;
import com.example.projetoAP.domain.vo.*;
import com.example.projetoAP.repository.*;

import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;

@Service
public class CalcularPerdas {

    private final RepositoryEntity repository;
    private final MetricasRepository metricasRepository;
    private final ICMPRepository icmpRepository;

    private final ExecutorService executor = Executors.newFixedThreadPool(20);
    private volatile boolean emExecucao = false;

    private static final long CACHE_TTL_MILLIS = 1000;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // 🔥 cache do ciclo atual (resultado do fping)
    private final ConcurrentHashMap<String, PingStats> resultadoAtual = new ConcurrentHashMap<>();

    private static final class CacheEntry {
        final Map<String, List<MetricasDTO>> result;
        final long timestamp;

        private CacheEntry(Map<String, List<MetricasDTO>> result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }
    }

    public CalcularPerdas(RepositoryEntity repository,
                          ICMPRepository icmpRepository,
                          MetricasRepository metricasRepository) {
        this.repository = repository;
        this.icmpRepository = icmpRepository;
        this.metricasRepository = metricasRepository;
    }

    @PreDestroy
    public void shutdownExecutor() {
        executor.shutdown();
    }

    private record PingStats(
            double latenciaMedia,
            double latenciaMin,
            double latenciaMax,
            double jitter,
            int perdaPercent
    ) {}

    // 🔥 AGENDADOR PRINCIPAL
    @Scheduled(fixedDelay = 20000)
    public void CalcularMestricasDeEquipamento() {

        if (emExecucao) return;
        emExecucao = true;

        try {
            List<EndpointEntity> equipamentos = repository.findAll();
            List<ItemsICMPEntity> icmp = icmpRepository.findAll();

            if (equipamentos.isEmpty()) return;

            // 🔥 1 - roda fping em lote (UMA VEZ)
            executarFpingLote(equipamentos);

            // 🔥 2 - mantém sua lógica original
            List<CompletableFuture<Void>> lista = new ArrayList<>();

            for (var eq : equipamentos) {
                lista.add(runFuntion(eq.getIp(), icmp, eq.getId()));
            }

            CompletableFuture.allOf(lista.toArray(new CompletableFuture[0])).join();

        } finally {
            resultadoAtual.clear();
            emExecucao = false;
        }
    }

    // 🔥 substitui o ping antigo
    private PingStats executarPingCompleto(String ip) {
        return resultadoAtual.getOrDefault(
                ip,
                new PingStats(0, 0, 0, 0, 100)
        );
    }

    // 🔥 motor com fping
    private void executarFpingLote(List<EndpointEntity> equipamentos) {

        try {
            List<String> comando = new ArrayList<>();
            comando.add("fping");
            comando.add("-c4");
            comando.add("-t2000");
            comando.add("-q");

            for (var eq : equipamentos) {
                comando.add(eq.getIp());
            }

            ProcessBuilder pb = new ProcessBuilder(comando);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream())
            );

            String linha;

            while ((linha = reader.readLine()) != null) {
                try {
                    String[] partes = linha.split(":");
                    String ip = partes[0].trim();

                    int loss = linha.contains("0%") ? 0 : 100;
                    double avg = 0;

                    if (linha.contains("min/avg/max")) {
                        String[] lat = linha.split("min/avg/max =")[1].trim().split("/");
                        avg = Double.parseDouble(lat[1]);
                    }

                    resultadoAtual.put(ip, new PingStats(avg, avg, avg, 0, loss));

                } catch (Exception ignored) {}
            }

            process.waitFor(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔥 NÃO ALTERADO (usa novo backend)
    public CompletableFuture<Void> runFuntion(String ip, List<ItemsICMPEntity> icmp, long hostid) {

        return CompletableFuture.runAsync(() -> {

            PingStats stats = executarPingCompleto(ip);

            List<MetricasEntity> batch = new ArrayList<>();

            for (var item : icmp) {

                if ("icmppingsec".equals(item.getKey_())) {

                    MetricasDTO m = new MetricasDTO(
                            String.valueOf(stats.latenciaMedia()),
                            item.getItemid(), hostid, item.getName(), 0L, ""
                    );

                    batch.add(new MetricasEntity(new Metricas(m)));

                } else if ("icmppingloss".equals(item.getKey_())) {

                    LostDTO m = new LostDTO(
                            String.valueOf(stats.perdaPercent()),
                            item.getItemid(), hostid, item.getName()
                    );

                    batch.add(new MetricasEntity(new Metricas(m)));
                }
            }

            // extras
            batch.add(new MetricasEntity(new Metricas(
                    new MetricasDTO(String.valueOf(stats.latenciaMin()), 0, hostid, "ICMP min latency", 0L, ""))));
            batch.add(new MetricasEntity(new Metricas(
                    new MetricasDTO(String.valueOf(stats.latenciaMax()), 0, hostid, "ICMP max latency", 0L, ""))));
            batch.add(new MetricasEntity(new Metricas(
                    new MetricasDTO(String.valueOf(stats.jitter()), 0, hostid, "ICMP jitter", 0L, ""))));

            if (!batch.isEmpty()) {
                metricasRepository.saveAll(batch);
            }

        }, executor);
    }

    // 🔥 RESTAURADO (igual você tinha)
    public Map<String, List<MetricasDTO>> ListaMestricasSemID(FilterHost filter) {

        int hours = (filter.hours() != null) ? filter.hours() : 0;
        int seconds = (filter.seconds() != null) ? filter.seconds() : 0;

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
            resultado = metricasRepository
                    .findTop5000ByHostidAndTimestampAfterOrderByTimestampDesc(filter.hostid(), desde);

        } else if (hours > 0) {
            LocalDateTime desde = LocalDateTime.now().minusHours(hours);
            resultado = metricasRepository
                    .findTop5000ByHostidAndTimestampAfterOrderByTimestampDesc(filter.hostid(), desde);

        } else {
            resultado = metricasRepository
                    .findTop5000ByHostidOrderByTimestampDesc(filter.hostid());
        }

        var lista = resultado.stream()
                .map(MetricasDTO::new)
                .collect(Collectors.toList());

        listaMap.put("result", lista);

        cache.put(cacheKey, new CacheEntry(listaMap, now));

        return listaMap;
    }

    public CompletableFuture<Map<String, List<MetricasDTO>>> listaMetricas(Filter filter) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, List<MetricasDTO>> listaMap = new HashMap<>();

            var lista = metricasRepository
                    .findTop2000ByItemidAndHostidOrderByIdDesc(filter.itemids(), filter.hostid())
                    .stream()
                    .map(MetricasDTO::new)
                    .collect(Collectors.toList());

            listaMap.put("result", lista);
            return listaMap;
        });
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deletedByTimestamp() {

        LocalDateTime timeDay = LocalDateTime.now().minus(2, ChronoUnit.DAYS);

        var lista = metricasRepository.findAllByTimestampBefore(timeDay);

        if (!lista.isEmpty()) {
            metricasRepository.deleteAll(lista);
        }
    }
}