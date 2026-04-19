package com.example.projetoAP.repository;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
    private String ipRecebido;
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

    // ===== Equipamento =====
    private String tipo;                 // LAPTOP / DESKTOP
    private String marca;
    private String modelo;
    private String numeroSerie;
    private String patrimonio;
    private LocalDate dataCompra;
    private LocalDate garantiaExpiracao;
    @Column(columnDefinition = "TEXT")
    private String descricao;
    private String imagemUrl;            // caminho/URL da imagem do equipamento
    private Integer numManutencoes = 0;  // contador de manutencoes
    private String statusUso = "NORMAL"; // NORMAL / USADO / CRITICO

    @Column(nullable = false)
    private boolean cadastroManual = false; // true quando cadastrado pela web (sem agente)

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
    public String getIpRecebido() { return ipRecebido; }
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
    public String getTipo() { return tipo; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public String getPatrimonio() { return patrimonio; }
    public LocalDate getDataCompra() { return dataCompra; }
    public LocalDate getGarantiaExpiracao() { return garantiaExpiracao; }
    public String getDescricao() { return descricao; }
    public String getImagemUrl() { return imagemUrl; }
    public Integer getNumManutencoes() { return numManutencoes == null ? 0 : numManutencoes; }
    public String getStatusUso() { return statusUso == null ? "NORMAL" : statusUso; }
    public boolean isCadastroManual() { return cadastroManual; }

    public void setHostname(String hostname) { this.hostname = hostname; }
    public void setIp(String ip) { this.ip = ip; }
    public void setIpRecebido(String ipRecebido) { this.ipRecebido = ipRecebido; }
    public void setSetor(String setor) { this.setor = setor; }
    public void setFilial(String filial) { this.filial = filial; }
    public void setAnydesk(String anydesk) { this.anydesk = anydesk; }
    public void setDominio(String dominio) { this.dominio = dominio; }
    public void setSistemaOperacional(String so) { this.sistemaOperacional = so; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public void setUltimoContato(LocalDateTime dt) { this.ultimoContato = dt; }
    public void setDataCadastro(LocalDateTime dt) { this.dataCadastro = dt; }
    public void setCpuPercent(Double v) { this.cpuPercent = v; }
    public void setMemoriaPercent(Double v) { this.memoriaPercent = v; }
    public void setMemoriaTotal(Long v) { this.memoriaTotal = v; }
    public void setMemoriaUsada(Long v) { this.memoriaUsada = v; }
    public void setDiscoTotal(Long v) { this.discoTotal = v; }
    public void setDiscoUsado(Long v) { this.discoUsado = v; }
    public void setStatus(String status) { this.status = status; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setMarca(String marca) { this.marca = marca; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    public void setPatrimonio(String patrimonio) { this.patrimonio = patrimonio; }
    public void setDataCompra(LocalDate dataCompra) { this.dataCompra = dataCompra; }
    public void setGarantiaExpiracao(LocalDate garantiaExpiracao) { this.garantiaExpiracao = garantiaExpiracao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setImagemUrl(String imagemUrl) { this.imagemUrl = imagemUrl; }
    public void setNumManutencoes(Integer numManutencoes) { this.numManutencoes = numManutencoes; }
    public void setStatusUso(String statusUso) { this.statusUso = statusUso; }
    public void setCadastroManual(boolean cadastroManual) { this.cadastroManual = cadastroManual; }

    // ===== Helpers de calculo =====
    public Integer anosDesdeCompra() {
        if (dataCompra == null) return null;
        return (int) ChronoUnit.YEARS.between(dataCompra, LocalDate.now());
    }

    public Integer diasParaExpirarGarantia() {
        if (garantiaExpiracao == null) return null;
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), garantiaExpiracao);
    }

    public boolean garantiaVencida() {
        return garantiaExpiracao != null && garantiaExpiracao.isBefore(LocalDate.now());
    }

    /** Calcula statusUso com base em numManutencoes e idade. */
    public void recalcularStatusUso() {
        int m = getNumManutencoes();
        Integer anos = anosDesdeCompra();
        if (m >= 10) {
            this.statusUso = "CRITICO";
        } else if (m >= 5 || (anos != null && anos >= 5)) {
            this.statusUso = "USADO";
        } else {
            this.statusUso = "NORMAL";
        }
    }
}
