package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FilialRepository extends JpaRepository<FilialEntity, Long> {
    Optional<FilialEntity> findByNome(String nome);
}
