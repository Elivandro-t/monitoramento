package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ManutencaoStatusLogRepository extends JpaRepository<ManutencaoStatusLogEntity, Long> {
    List<ManutencaoStatusLogEntity> findByManutencaoIdOrderByDataHoraDesc(long manutencaoId);
    List<ManutencaoStatusLogEntity> findByAgentIdOrderByDataHoraDesc(String agentId);
}
