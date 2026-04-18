package com.example.projetoAP.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_usuario", columnList = "usuario"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_acao", columnList = "acao")
})
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String usuario;
    private String acao;
    private String rota;

    @Column(length = 2000)
    private String dados;

    private LocalDateTime timestamp;
    private String ipMaquina;

    @Column(length = 500)
    private String userAgent;

    private String navegador;
    private String sistemaOperacional;

    public AuditLogEntity() {}

    public AuditLogEntity(String usuario, String acao, String rota, String dados,
                          String ipMaquina, String userAgent) {
        this.usuario = usuario;
        this.acao = acao;
        this.rota = rota;
        this.dados = dados;
        this.timestamp = LocalDateTime.now();
        this.ipMaquina = ipMaquina;
        this.userAgent = userAgent;
        this.navegador = extrairNavegador(userAgent);
        this.sistemaOperacional = extrairSO(userAgent);
    }

    private String extrairNavegador(String ua) {
        if (ua == null) return "Desconhecido";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Edg")) return "Edge";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Safari")) return "Safari";
        if (ua.contains("Opera") || ua.contains("OPR")) return "Opera";
        return "Outro";
    }

    private String extrairSO(String ua) {
        if (ua == null) return "Desconhecido";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        return "Outro";
    }

    public long getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getAcao() { return acao; }
    public String getRota() { return rota; }
    public String getDados() { return dados; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getIpMaquina() { return ipMaquina; }
    public String getUserAgent() { return userAgent; }
    public String getNavegador() { return navegador; }
    public String getSistemaOperacional() { return sistemaOperacional; }
}
