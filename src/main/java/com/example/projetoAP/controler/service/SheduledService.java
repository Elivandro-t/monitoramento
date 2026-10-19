package com.example.projetoAP.controler.service;

import com.example.projetoAP.controler.EndpointProcessorService;
import com.example.projetoAP.repository.RepositoryEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SheduledService {

    private final RepositoryEntity entityRepository;

    private final EndpointProcessorService processorService;

    private final EndpointService endpointService;

    private final SimpMessagingTemplate messagingTemplate;

    private final AtomicBoolean emExecucao =
            new AtomicBoolean(false);

    public SheduledService(
            RepositoryEntity entityRepository,
            EndpointProcessorService processorService,
            EndpointService endpointService,
            SimpMessagingTemplate messagingTemplate
    ) {

        this.entityRepository = entityRepository;

        this.processorService = processorService;

        this.endpointService = endpointService;

        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(cron = "0 */1 * * * *")
    @Scheduled(cron = "0 */1 * * * *")
    public void gerar() {

        if (!emExecucao.compareAndSet(false, true)) {

            System.out.println("⏭️ Scheduler já em execução");
            return;
        }

        try {

            List<Long> ids = entityRepository
                    .findAll()
                    .stream()
                    .map(endpoint -> endpoint.getId())
                    .toList();

            int batchSize = 10; // 👈 limite REAL de concorrência segura

            for (int i = 0; i < ids.size(); i += batchSize) {

                List<Long> lote = ids.subList(
                        i,
                        Math.min(i + batchSize, ids.size())
                );

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (Long id : lote) {
                    futures.add(
                            processorService.processarEndpoint(id)
                    );
                }

                CompletableFuture
                        .allOf(futures.toArray(new CompletableFuture[0]))
                        .join();
            }

        } catch (Exception e) {

            System.err.println("❌ Erro scheduler: " + e.getMessage());
            e.printStackTrace();

        } finally {

            emExecucao.set(false);
        }
    }

    @Scheduled(cron = "*/30 * * * * *")
    public void getVierListPage() {

        var lista = endpointService.getLista();

        messagingTemplate.convertAndSend(
                "/lista",
                lista
        );
    }
}