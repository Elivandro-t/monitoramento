package com.example.projetoAP.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agente", indexes = {
        @Index(name = "idx_agente_agent_id", columnList = "agentId")
})
public class AgenteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, nullable = false)
    private String agentId;

    private String hostname;
    private String ip;
    private String setor;
    private String filial;
    private String anydesk;
    private String dominio;
    private String sistemaOperacional;
    private String usuario;

    @Column(nullable = false)
    private boolean ativo = false;

    private LocalDateTime ultimoContato;
    private LocalDateTime dataCadastro;

    // Metricas mais recentes
    private Double cpuPercent;
    private Double memoriaPercent;
    private Long memoriaTotal;
    private Long memoriaUsada;
    private Long discoTotal;
    private Long discoUsado;
    private String status;

    public AgenteEntity() {}

    public AgenteEntity(String agentId, String hostname, String setor, String filial,
                        String anydesk, String dominio) {
        this.agentId = agentId;
        this.hostname = hostname;
        this.setor = setor;
        this.filial = filial;
        this.anydesk = anydesk;
        this.dominio = dominio;
        this.dataCadastro = LocalDateTime.now();
        this.ativo = false;
        this.status = "OFFLINE";
    }

    public long getId() { return id; }
    public String getAgentId() { return agentId; }
    public String getHostname() { return hostname; }
    public String getIp() { return ip; }
    public String getSetor() { return setor; }
    public String getFilial() { return filial; }
    public String getAnydesk() { return anydesk; }
    public String getDominio() { return dominio; }
    public String getSistemaOperacional() { return sistemaOperacional; }
    public String getUsuario() { return usuario; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getUltimoContato() { return ultimoContato; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public Double getCpuPercent() { return cpuPercent; }
    public Double getMemoriaPercent() { return memoriaPercent; }
    public Long getMemoriaTotal() { return memoriaTotal; }
    public Long getMemoriaUsada() { return memoriaUsada; }
    public Long getDiscoTotal() { return discoTotal; }
    public Long getDiscoUsado() { return discoUsado; }
    public String getStatus() { return status; }

    public void setHostname(String hostname) { this.hostname = hostname; }
    public void setIp(String ip) { this.ip = ip; }
    public void setSetor(String setor) { this.setor = setor; }
    public void setFilial(String filial) { this.filial = filial; }
    public void setAnydesk(String anydesk) { this.anydesk = anydesk; }
    public void setDominio(String dominio) { this.dominio = dominio; }
    public void setSistemaOperacional(String so) { this.sistemaOperacional = so; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public void setUltimoContato(LocalDateTime dt) { this.ultimoContato = dt; }
    public void setCpuPercent(Double v) { this.cpuPercent = v; }
    public void setMemoriaPercent(Double v) { this.memoriaPercent = v; }
    public void setMemoriaTotal(Long v) { this.memoriaTotal = v; }
    public void setMemoriaUsada(Long v) { this.memoriaUsada = v; }
    public void setDiscoTotal(Long v) { this.discoTotal = v; }
    public void setDiscoUsado(Long v) { this.discoUsado = v; }
    public void setStatus(String status) { this.status = status; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
}
