package com.example.projetoAP.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manutencao_status_log", indexes = {
        @Index(name = "idx_msl_manutencao", columnList = "manutencaoId"),
        @Index(name = "idx_msl_agent", columnList = "agentId")
})
public class ManutencaoStatusLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private long manutencaoId;

    @Column(nullable = false)
    private String agentId;

    private String statusAnterior;

    @Column(nullable = false)
    private String statusNovo;

    @Column(nullable = false)
    private String usuario;

    private String usuarioNome;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    public ManutencaoStatusLogEntity() {}

    public ManutencaoStatusLogEntity(long manutencaoId, String agentId, String statusAnterior,
                                     String statusNovo, String usuario, String usuarioNome, String observacao) {
        this.manutencaoId = manutencaoId;
        this.agentId = agentId;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.usuario = usuario;
        this.usuarioNome = usuarioNome;
        this.observacao = observacao;
        this.dataHora = LocalDateTime.now();
    }

    public long getId() { return id; }
    public long getManutencaoId() { return manutencaoId; }
    public String getAgentId() { return agentId; }
    public String getStatusAnterior() { return statusAnterior; }
    public String getStatusNovo() { return statusNovo; }
    public String getUsuario() { return usuario; }
    public String getUsuarioNome() { return usuarioNome; }
    public String getObservacao() { return observacao; }
    public LocalDateTime getDataHora() { return dataHora; }

    public void setManutencaoId(long manutencaoId) { this.manutencaoId = manutencaoId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public void setStatusAnterior(String statusAnterior) { this.statusAnterior = statusAnterior; }
    public void setStatusNovo(String statusNovo) { this.statusNovo = statusNovo; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
