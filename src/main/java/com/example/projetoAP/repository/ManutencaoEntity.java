package com.example.projetoAP.repository;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "manutencao", indexes = {
        @Index(name = "idx_manutencao_agent_id", columnList = "agentId"),
        @Index(name = "idx_manutencao_status", columnList = "status")
})
public class ManutencaoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String agentId;

    @Column(nullable = false)
    private String tecnicoUsername;

    private String tecnicoNome;

    // Responsavel atual (pode mudar ao longo da manutencao)
    private String responsavelUsername;
    private String responsavelNome;

    // Snapshot para filtros na tela /manutencoes
    private String setor;
    private String filial;
    private String patrimonio;
    private String hostname;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(columnDefinition = "TEXT")
    private String laudoFinal;

    private String anexoArquivo;
    private String anexoOriginal;

    private String imagem;

    // AGUARDANDO_MANUTENCAO | PENDENTE | AGUARDANDO_PECAS | CONDENADO | FINALIZADO | ENTREGUE
    @Column(nullable = false)
    private String status = "AGUARDANDO_MANUTENCAO";

    // PREVENTIVA | CORRETIVA
    private String tipoManutencao;

    @Column(nullable = false)
    private LocalDateTime dataManutencao;

    private LocalDateTime dataEntrada;
    private LocalDateTime dataEntrega;

    public ManutencaoEntity() {}

    public long getId() { return id; }
    public String getAgentId() { return agentId; }
    public String getTecnicoUsername() { return tecnicoUsername; }
    public String getTecnicoNome() { return tecnicoNome; }
    public String getResponsavelUsername() { return responsavelUsername; }
    public String getResponsavelNome() { return responsavelNome; }
    public String getSetor() { return setor; }
    public String getFilial() { return filial; }
    public String getPatrimonio() { return patrimonio; }
    public String getHostname() { return hostname; }
    public String getDescricao() { return descricao; }
    public String getLaudoFinal() { return laudoFinal; }
    public String getAnexoArquivo() { return anexoArquivo; }
    public String getAnexoOriginal() { return anexoOriginal; }
    public String getImagem() { return imagem; }
    public String getStatus() { return status; }
    public String getTipoManutencao() { return tipoManutencao; }
    public LocalDateTime getDataManutencao() { return dataManutencao; }
    public LocalDateTime getDataEntrada() { return dataEntrada; }
    public LocalDateTime getDataEntrega() { return dataEntrega; }

    public void setAgentId(String agentId) { this.agentId = agentId; }
    public void setTecnicoUsername(String tecnicoUsername) { this.tecnicoUsername = tecnicoUsername; }
    public void setTecnicoNome(String tecnicoNome) { this.tecnicoNome = tecnicoNome; }
    public void setResponsavelUsername(String responsavelUsername) { this.responsavelUsername = responsavelUsername; }
    public void setResponsavelNome(String responsavelNome) { this.responsavelNome = responsavelNome; }
    public void setSetor(String setor) { this.setor = setor; }
    public void setFilial(String filial) { this.filial = filial; }
    public void setPatrimonio(String patrimonio) { this.patrimonio = patrimonio; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setLaudoFinal(String laudoFinal) { this.laudoFinal = laudoFinal; }
    public void setAnexoArquivo(String anexoArquivo) { this.anexoArquivo = anexoArquivo; }
    public void setAnexoOriginal(String anexoOriginal) { this.anexoOriginal = anexoOriginal; }
    public void setImagem(String imagem) { this.imagem = imagem; }
    public void setStatus(String status) { this.status = status; }
    public void setTipoManutencao(String tipoManutencao) { this.tipoManutencao = tipoManutencao; }
    public void setDataManutencao(LocalDateTime dataManutencao) { this.dataManutencao = dataManutencao; }
    public void setDataEntrada(LocalDateTime dataEntrada) { this.dataEntrada = dataEntrada; }
    public void setDataEntrega(LocalDateTime dataEntrega) { this.dataEntrega = dataEntrega; }

    /** Duracao desde dataEntrada ate dataEntrega (ou agora, se em andamento). Null se sem dataEntrada. */
    public Duration duracao() {
        if (dataEntrada == null) return null;
        LocalDateTime fim = dataEntrega != null ? dataEntrega : LocalDateTime.now();
        return Duration.between(dataEntrada, fim);
    }

    /** Alerta se tempo > 2 dias e status != AGUARDANDO_PECAS. */
    public boolean alertaTempo() {
        if ("AGUARDANDO_PECAS".equals(status)) return false;
        if ("ENTREGUE".equals(status) || "FINALIZADO".equals(status) || "CONDENADO".equals(status)) return false;
        Duration d = duracao();
        return d != null && d.toDays() > 2;
    }
}
