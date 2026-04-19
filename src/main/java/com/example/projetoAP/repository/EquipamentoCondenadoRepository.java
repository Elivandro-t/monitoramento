package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipamentoCondenadoRepository extends JpaRepository<EquipamentoCondenadoEntity, Long> {
    List<EquipamentoCondenadoEntity> findAllByOrderByDataCondenacaoDesc();
    boolean existsByAgentId(String agentId);
}
