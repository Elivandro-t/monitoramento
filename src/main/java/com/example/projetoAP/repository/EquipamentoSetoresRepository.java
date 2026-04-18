package com.example.projetoAP.repository;

import com.example.projetoAP.repository.EquipamentoSetores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipamentoSetoresRepository extends JpaRepository<EquipamentoSetores, Long> {
    // Métodos personalizados podem ser adicionados aqui, se necessário
}