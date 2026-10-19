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

@Service
public class CalcularPerdas {

    private final RepositoryEntity repository;
    private final MetricasRepository metricasRepository;
    private final ICMPRepository icmpRepository;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
    private static final java.util.regex.Pattern LOSS = java.util.regex.Pattern.compile("/(\\d+)%");
    private static final java.util.regex.Pattern IPV4_OU_HOST = java.util.regex.Pattern.compile("^[A-Za-z0-9.\\-]+$");
    private volatile boolean emExecucao = false;

    private final ConcurrentHashMap<String, PingStats> resultadoAtual = new ConcurrentHashMap<>();

    private static final long CACHE_TTL_MILLIS = 2000;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

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
        watchdog.shutdownNow();
    }

    private record PingStats(double latencia, int perda) {}

    private static final class CacheEntry {
        final Map<String, List<MetricasDTO>> result;
        final long timestamp;

        private CacheEntry(Map<String, List<MetricasDTO>> result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }
    }

    // 🔥 AGENDADOR PRINCIPAL
    @Scheduled(fixedDelay = 8000)
    public void CalcularMestricasDeEquipamento() {

        if (emExecucao) return;
        emExecucao = true;

        try {

            List<EndpointEntity> equipamentos = repository.findAll();
            List<ItemsICMPEntity> icmp = icmpRepository.findAll();

            if (equipamentos.isEmpty()) return;

            // 🔥 roda fping UMA VEZ
            executarFpingLote(equipamentos);

            List<MetricasEntity> tudo = Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (var eq : equipamentos) {

                futures.add(CompletableFuture.runAsync(() -> {

                    PingStats stats = resultadoAtual.getOrDefault(
                            eq.getIp(),
                            new PingStats(0, 100)
                    );

                    List<MetricasEntity> batch = new ArrayList<>();

                    for (var item : icmp) {

                        if ("icmppingsec".equals(item.getKey_())) {
                            batch.add(new MetricasEntity(new Metricas(
                                    new MetricasDTO(
                                            String.valueOf(stats.latencia),
                                            item.getItemid(),
                                            eq.getId(),
                                            item.getName(),
                                            0L,
                                            ""
                                    )
                            )));
                        }

                        if ("icmppingloss".equals(item.getKey_())) {
                            batch.add(new MetricasEntity(new Metricas(
                                    new LostDTO(
                                            String.valueOf(stats.perda),
                                            item.getItemid(),
                                            eq.getId(),
                                            item.getName()
                                    )
                            )));
                        }
                    }

                    tudo.addAll(batch);

                }, executor));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 🔥 salva UMA vez (MUITO MAIS RÁPIDO)
            if (!tudo.isEmpty()) {
                metricasRepository.saveAll(tudo);
            }

        } finally {
            resultadoAtual.clear();
            emExecucao = false;
        }
    }

    // 🔥 FPING EM LOTE
    private void executarFpingLote(List<EndpointEntity> equipamentos) {

        int BATCH_SIZE = 200; // ajuste se quiser (100~300 ideal)

        for (int i = 0; i < equipamentos.size(); i += BATCH_SIZE) {

            List<EndpointEntity> sub = equipamentos.subList(
                    i,
                    Math.min(i + BATCH_SIZE, equipamentos.size())
            );

            try {

                List<String> comando = new ArrayList<>();
                comando.add("fping");
                comando.add("-c1");
                comando.add("-t500");
                comando.add("-q");

                for (var eq : sub) {
                    String ip = eq.getIp() == null ? "" : eq.getIp().trim();
                    if (IPV4_OU_HOST.matcher(ip).matches()) {
                        comando.add(ip);
                    }
                }
                if (comando.size() == 4) continue;

                ProcessBuilder pb = new ProcessBuilder(comando);
                pb.redirectErrorStream(true);

                Process process = pb.start();
                // se o fping travar, mata o processo (senao o readLine bloqueia e o servico para)
                ScheduledFuture<?> kill = watchdog.schedule(process::destroyForcibly, 20, TimeUnit.SECONDS);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                );

                String linha;

                while ((linha = reader.readLine()) != null) {
                    try {
                        String[] partes = linha.split(":");
                        String ip = partes[0].trim();

                        // "100%" contem "0%": le o numero real do %loss
                        var m = LOSS.matcher(linha);
                        int perda = m.find() ? Integer.parseInt(m.group(1)) : 100;

                        double latencia = 0;

                        if (linha.contains("min/avg/max")) {
                            String[] lat = linha.split("min/avg/max =")[1].trim().split("/");
                            latencia = Double.parseDouble(lat[1]);
                        }

                        resultadoAtual.put(ip, new PingStats(latencia, perda));

                    } catch (Exception ignored) {}
                }

                kill.cancel(false);
                if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 🔥 NÃO MEXI — SOCKET USA ISSO
    public CompletableFuture<Map<String, List<MetricasDTO>>> listaMetricas(Filter filter) {
        return CompletableFuture.supplyAsync(() -> {

            Map<String, List<MetricasDTO>> listaMap = new HashMap<>();

            var lista = metricasRepository
                    .findTop2000ByItemidAndHostidOrderByIdDesc(
                            filter.itemids(),
                            filter.hostid()
                    )
                    .stream()
                    .map(MetricasDTO::new)
                    .collect(Collectors.toList());

            listaMap.put("result", lista);

            return listaMap;
        });
    }

    // 🔥 RESTAURADO (grafico sem itemid)
    public Map<String, List<MetricasDTO>> ListaMestricasSemID(FilterHost filter) {

        int hours = (filter.hours() != null) ? filter.hours() : 0;
        int seconds = (filter.seconds() != null) ? filter.seconds() : 0;

        String cacheKey = filter.hostid() + ":" + hours + ":" + seconds;

        long now = System.currentTimeMillis();

        CacheEntry cached = cache.get(cacheKey);

        if (cached != null && now - cached.timestamp <= CACHE_TTL_MILLIS) {
            return cached.result;
        }

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

        Map<String, List<MetricasDTO>> listaMap = new HashMap<>();
        listaMap.put("result", lista);

        cache.put(cacheKey, new CacheEntry(listaMap, now));

        return listaMap;
    }

    // Apaga em lotes pequenos: nada de carregar milhoes de linhas em memoria
    // nem segurar uma transacao gigante que trava os inserts.
    @Scheduled(cron = "0 0 0 * * *")
    public void deletedByTimestamp() {

        LocalDateTime timeDay = LocalDateTime.now().minus(2, ChronoUnit.DAYS);

        int apagados;
        do {
            apagados = metricasRepository.deleteLoteAntes(timeDay);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        } while (apagados > 0);
    }
}