package com.example.projetoAP.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manutencao", indexes = {
        @Index(name = "idx_manutencao_agent_id", columnList = "agentId")
})
public class ManutencaoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String agentId;         // referencia ao AgenteEntity.agentId

    @Column(nullable = false)
    private String tecnicoUsername; // usuario logado (tecnico) que fez a manutencao

    private String tecnicoNome;

    @Column(columnDefinition = "TEXT")
    private String descricao;       // item descricao

    @Column(columnDefinition = "TEXT")
    private String laudoFinal;      // laudo final textual

    private String anexoArquivo;    // caminho relativo do laudo anexado (pdf, etc)
    private String anexoOriginal;   // nome original do arquivo

    private String imagem;          // caminho da imagem do equipamento

    @Column(nullable = false)
    private LocalDateTime dataManutencao;

    public ManutencaoEntity() {}

    public long getId() { return id; }
    public String getAgentId() { return agentId; }
    public String getTecnicoUsername() { return tecnicoUsername; }
    public String getTecnicoNome() { return tecnicoNome; }
    public String getDescricao() { return descricao; }
    public String getLaudoFinal() { return laudoFinal; }
    public String getAnexoArquivo() { return anexoArquivo; }
    public String getAnexoOriginal() { return anexoOriginal; }
    public String getImagem() { return imagem; }
    public LocalDateTime getDataManutencao() { return dataManutencao; }

    public void setAgentId(String agentId) { this.agentId = agentId; }
    public void setTecnicoUsername(String tecnicoUsername) { this.tecnicoUsername = tecnicoUsername; }
    public void setTecnicoNome(String tecnicoNome) { this.tecnicoNome = tecnicoNome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setLaudoFinal(String laudoFinal) { this.laudoFinal = laudoFinal; }
    public void setAnexoArquivo(String anexoArquivo) { this.anexoArquivo = anexoArquivo; }
    public void setAnexoOriginal(String anexoOriginal) { this.anexoOriginal = anexoOriginal; }
    public void setImagem(String imagem) { this.imagem = imagem; }
    public void setDataManutencao(LocalDateTime dataManutencao) { this.dataManutencao = dataManutencao; }
}
