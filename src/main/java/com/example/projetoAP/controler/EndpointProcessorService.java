package com.example.projetoAP.controler;

import com.example.projetoAP.repository.EndpointEntity;
import com.example.projetoAP.repository.RepositoryEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class EndpointProcessorService {

    private final RepositoryEntity entityRepository;

    private final String os =
            System.getProperty("os.name").toLowerCase();

    public EndpointProcessorService(
            RepositoryEntity entityRepository
    ) {
        this.entityRepository = entityRepository;
    }

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> processarEndpoint(Long id) {

        try {

            EndpointEntity endpoint =
                    entityRepository
                            .findById(id)
                            .orElseThrow();

            PingResult pingResult =
                    executarPing(endpoint.getIp());

            String statusAnterior =
                    endpoint.getStatus();

            LocalDateTime agora =
                    LocalDateTime.now();

            LocalDateTime ultimaVezOnline =
                    endpoint.getUltimaVezOnline();

            boolean agoraIntermitente = false;

            if ("UP".equals(pingResult.status())) {

                if (pingResult.latenciaMedia() != null
                        && pingResult.latenciaMedia() > 150) {

                    agoraIntermitente = true;

                } else if ("DOWN".equals(statusAnterior)
                        && ultimaVezOnline != null) {

                    long segundosOffline =
                            Duration.between(
                                    ultimaVezOnline,
                                    agora
                            ).getSeconds();

                    if (segundosOffline <= 60) {
                        agoraIntermitente = true;
                    }
                }
            }

            boolean mudouStatus =
                    !Objects.equals(
                            statusAnterior,
                            pingResult.status()
                    );

            endpoint.setStatus(
                    pingResult.status()
            );

            if ("UP".equals(pingResult.status())) {

                endpoint.setUltimaVezOnline(
                        agora
                );
            }

            if (mudouStatus) {

                entityRepository.save(endpoint);
            }

        } catch (Exception e) {

            System.err.println(
                    "❌ Erro endpoint "
                            + id
                            + ": "
                            + e.getMessage()
            );
        }

        return CompletableFuture.completedFuture(null);
    }

    private PingResult executarPing(String ip)
            throws IOException, InterruptedException {

        String[] comando;

        if (os.contains("win")) {

            comando = new String[]{
                    "cmd",
                    "/c",
                    "ping -n 1 -w 1500 " + ip
            };

        } else {

            comando = new String[]{
                    "sh",
                    "-c",
                    "fping -c 1 -t 1500 " + ip + " 2>&1"
            };
        }

        Process process =
                Runtime.getRuntime()
                        .exec(comando);

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream()
                            )
                    );

            StringBuilder detalhes =
                    new StringBuilder();

            boolean temResposta = false;

            Double latenciaMedia = null;

            String linha;

            while ((linha = reader.readLine()) != null) {

                detalhes
                        .append(linha)
                        .append("\n");

                if (linha.contains("min/avg/max")) {

                    temResposta = true;

                    try {

                        String[] partes =
                                linha.split("=");

                        if (partes.length >= 2) {

                            String estatisticas =
                                    partes[1].trim();

                            String[] valores =
                                    estatisticas.split("/");

                            if (valores.length >= 2) {

                                latenciaMedia =
                                        Double.parseDouble(
                                                valores[1]
                                        );
                            }
                        }

                    } catch (Exception ignored) {
                    }
                }

                if (linha.contains("100% loss")
                        || linha.contains("unreachable")
                        || linha.contains("timed out")) {

                    temResposta = false;
                }
            }

            reader.close();

            boolean terminou =
                    process.waitFor(
                            3,
                            TimeUnit.SECONDS
                    );

            if (!terminou) {

                process.destroyForcibly();

                return new PingResult(
                        "DOWN",
                        "Timeout",
                        null
                );
            }

            String status =
                    temResposta
                            ? "UP"
                            : "DOWN";

            return new PingResult(
                    status,
                    detalhes.toString().trim(),
                    latenciaMedia
            );

        } finally {

            process.destroy();
        }
    }

    private record PingResult(
            String status,
            String detalhes,
            Double latenciaMedia
    ) {
    }
}