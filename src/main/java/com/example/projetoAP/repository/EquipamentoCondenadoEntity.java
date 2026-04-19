package com.example.projetoAP.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipamento_condenado")
public class EquipamentoCondenadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String agentId;
    private String hostname;
    private String patrimonio;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private String setor;
    private String filial;
    private String tipo;

    @Column(columnDefinition = "TEXT")
    private String motivoCondenacao;

    @Column(columnDefinition = "TEXT")
    private String laudoFinal;

    @Column(columnDefinition = "TEXT")
    private String historicoManutencoes;  // JSON serializado das manutencoes

    // Caminho da imagem principal do equipamento no momento da condenacao
    @Column(length = 500)
    private String imagemPrincipal;

    private Integer totalManutencoes;
    private String condenadoPor;
    private LocalDateTime dataCondenacao;

    public EquipamentoCondenadoEntity() {}

    public long getId() { return id; }
    public String getAgentId() { return agentId; }
    public String getHostname() { return hostname; }
    public String getPatrimonio() { return patrimonio; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public String getSetor() { return setor; }
    public String getFilial() { return filial; }
    public String getTipo() { return tipo; }
    public String getMotivoCondenacao() { return motivoCondenacao; }
    public String getLaudoFinal() { return laudoFinal; }
    public String getHistoricoManutencoes() { return historicoManutencoes; }
    public String getImagemPrincipal() { return imagemPrincipal; }
    public Integer getTotalManutencoes() { return totalManutencoes; }
    public String getCondenadoPor() { return condenadoPor; }
    public LocalDateTime getDataCondenacao() { return dataCondenacao; }

    public void setAgentId(String agentId) { this.agentId = agentId; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public void setPatrimonio(String patrimonio) { this.patrimonio = patrimonio; }
    public void setMarca(String marca) { this.marca = marca; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    public void setSetor(String setor) { this.setor = setor; }
    public void setFilial(String filial) { this.filial = filial; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setMotivoCondenacao(String motivoCondenacao) { this.motivoCondenacao = motivoCondenacao; }
    public void setLaudoFinal(String laudoFinal) { this.laudoFinal = laudoFinal; }
    public void setHistoricoManutencoes(String historicoManutencoes) { this.historicoManutencoes = historicoManutencoes; }
    public void setImagemPrincipal(String imagemPrincipal) { this.imagemPrincipal = imagemPrincipal; }
    public void setTotalManutencoes(Integer totalManutencoes) { this.totalManutencoes = totalManutencoes; }
    public void setCondenadoPor(String condenadoPor) { this.condenadoPor = condenadoPor; }
    public void setDataCondenacao(LocalDateTime dataCondenacao) { this.dataCondenacao = dataCondenacao; }
}
