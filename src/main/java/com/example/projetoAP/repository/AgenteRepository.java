package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AgenteRepository extends JpaRepository<AgenteEntity, Long> {
    Optional<AgenteEntity> findByAgentId(String agentId);
    List<AgenteEntity> findByAtivoTrue();
    List<AgenteEntity> findBySetor(String setor);
    List<AgenteEntity> findByFilial(String filial);
    List<AgenteEntity> findBySetorAndFilial(String setor, String filial);
}
