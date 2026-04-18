package com.example.projetoAP.controler.service;

import com.example.projetoAP.repository.AuditLogEntity;
import com.example.projetoAP.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void registrar(String usuario, String acao, String rota, String dados,
                          String ipMaquina, String userAgent) {
        var log = new AuditLogEntity(usuario, acao, rota, dados, ipMaquina, userAgent);
        auditLogRepository.save(log);
    }

    public List<AuditLogEntity> listarTodos() {
        return auditLogRepository.findTop5000ByOrderByTimestampDesc();
    }

    public List<AuditLogEntity> filtrar(String usuario, String acao, LocalDateTime inicio, LocalDateTime fim) {
        if (usuario != null && !usuario.isBlank() && acao != null && !acao.isBlank()
                && inicio != null && fim != null) {
            return auditLogRepository.findByUsuarioContainingIgnoreCaseAndAcaoAndTimestampBetweenOrderByTimestampDesc(
                    usuario, acao, inicio, fim);
        }
        if (usuario != null && !usuario.isBlank()) {
            return auditLogRepository.findByUsuarioContainingIgnoreCaseOrderByTimestampDesc(usuario);
        }
        if (acao != null && !acao.isBlank()) {
            return auditLogRepository.findByAcaoOrderByTimestampDesc(acao);
        }
        if (inicio != null && fim != null) {
            return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(inicio, fim);
        }
        return listarTodos();
    }

    @Scheduled(cron = "0 0 2 1 */2 *")
    @Transactional
    public void limparLogsAntigos() {
        LocalDateTime limite = LocalDateTime.now().minusDays(60);
        var antigos = auditLogRepository.findAllByTimestampBefore(limite);
        if (!antigos.isEmpty()) {
            auditLogRepository.deleteAll(antigos);
        }
    }
}
