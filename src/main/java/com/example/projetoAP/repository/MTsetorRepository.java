package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MTsetorRepository extends JpaRepository<MTsetor, Long> {
    Optional<MTsetor> findByNome(String nome);
}
