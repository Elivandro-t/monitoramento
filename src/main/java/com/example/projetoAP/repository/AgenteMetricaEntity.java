package com.example.projetoAP.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agente_metrica", indexes = {
        @Index(name = "idx_agmetrica_agentid", columnList = "agentId"),
        @Index(name = "idx_agmetrica_timestamp", columnList = "timestamp")
})
public class AgenteMetricaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String agentId;
    private Double cpuPercent;
    private Double memoriaPercent;
    private Long memoriaUsada;
    private String status;
    private LocalDateTime timestamp;
    private String tempoUso;

    public AgenteMetricaEntity() {}

    public AgenteMetricaEntity(String agentId, Double cpuPercent, Double memoriaPercent,
                               Long memoriaUsada, String status,String tempoUso) {
        this.agentId = agentId;
        this.cpuPercent = cpuPercent;
        this.memoriaPercent = memoriaPercent;
        this.memoriaUsada = memoriaUsada;
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.tempoUso = tempoUso;
    }

    public long getId() { return id; }
    public String getAgentId() { return agentId; }
    public Double getCpuPercent() { return cpuPercent; }
    public Double getMemoriaPercent() { return memoriaPercent; }
    public Long getMemoriaUsada() { return memoriaUsada; }
    public String getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public String getTempoUso() {
        return tempoUso;
    }
}
