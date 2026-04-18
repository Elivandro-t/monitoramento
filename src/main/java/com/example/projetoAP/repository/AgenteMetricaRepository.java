package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AgenteMetricaRepository extends JpaRepository<AgenteMetricaEntity, Long> {
    List<AgenteMetricaEntity> findTop500ByAgentIdOrderByTimestampDesc(String agentId);
    List<AgenteMetricaEntity> findByAgentIdAndTimestampAfterOrderByTimestampDesc(String agentId, LocalDateTime desde);
    List<AgenteMetricaEntity> findAllByTimestampBefore(LocalDateTime timestamp);
}
