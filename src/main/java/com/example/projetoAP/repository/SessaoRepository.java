package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SessaoRepository extends JpaRepository<SessaoEntity, Long> {
    Optional<SessaoEntity> findByToken(String token);
    List<SessaoEntity> findByAtivaTrue();
    List<SessaoEntity> findByUsuarioOrderByHoraEntradaDesc(String usuario);
    List<SessaoEntity> findTop100ByOrderByHoraEntradaDesc();
}
