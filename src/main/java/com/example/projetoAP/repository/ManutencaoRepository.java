package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ManutencaoRepository extends JpaRepository<ManutencaoEntity, Long> {
    List<ManutencaoEntity> findByAgentIdOrderByDataManutencaoDesc(String agentId);
    long countByAgentId(String agentId);

    List<ManutencaoEntity> findByStatusNotOrderByDataManutencaoDesc(String status);
    List<ManutencaoEntity> findByStatusOrderByDataManutencaoDesc(String status);

    @Query("SELECT m FROM ManutencaoEntity m WHERE m.status <> 'CONDENADO' " +
            "AND (:status IS NULL OR m.status = :status) " +
            "AND (:setor IS NULL OR LOWER(m.setor) = LOWER(:setor)) " +
            "AND (:responsavel IS NULL OR LOWER(m.responsavelUsername) = LOWER(:responsavel)) " +
            "ORDER BY m.dataManutencao DESC")
    List<ManutencaoEntity> filtrarAtivas(@Param("status") String status,
                                         @Param("setor") String setor,
                                         @Param("responsavel") String responsavel);

    @Query("SELECT m.tecnicoUsername, COUNT(m) FROM ManutencaoEntity m " +
            "WHERE m.dataManutencao BETWEEN :inicio AND :fim " +
            "GROUP BY m.tecnicoUsername ORDER BY COUNT(m) DESC")
    List<Object[]> rankingPorPeriodo(@Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim);

    long countByTecnicoUsername(String tecnicoUsername);
    List<ManutencaoEntity> findByTecnicoUsernameOrderByDataManutencaoDesc(String tecnicoUsername);
}
