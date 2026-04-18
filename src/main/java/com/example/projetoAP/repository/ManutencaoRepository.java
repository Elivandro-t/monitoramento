package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ManutencaoRepository extends JpaRepository<ManutencaoEntity, Long> {
    List<ManutencaoEntity> findByAgentIdOrderByDataManutencaoDesc(String agentId);
    long countByAgentId(String agentId);
}
