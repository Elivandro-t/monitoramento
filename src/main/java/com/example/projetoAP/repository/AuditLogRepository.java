package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    List<AuditLogEntity> findTop5000ByOrderByTimestampDesc();
    List<AuditLogEntity> findByUsuarioContainingIgnoreCaseOrderByTimestampDesc(String usuario);
    List<AuditLogEntity> findByAcaoOrderByTimestampDesc(String acao);
    List<AuditLogEntity> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime inicio, LocalDateTime fim);
    List<AuditLogEntity> findByUsuarioContainingIgnoreCaseAndAcaoAndTimestampBetweenOrderByTimestampDesc(
            String usuario, String acao, LocalDateTime inicio, LocalDateTime fim);
    List<AuditLogEntity> findAllByTimestampBefore(LocalDateTime timestamp);
}
