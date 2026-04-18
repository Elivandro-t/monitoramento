package com.example.projetoAP.dto;

public class EquipamentoDTO {

    private String tipo;
    private String dataCompra;
    private String garantiaExpiracao;
    private String statusUso;
    private String imagemUrl;
    private String descricao;

    // Getters e Setters
    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDataCompra() {
        return dataCompra;
    }

    public void setDataCompra(String dataCompra) {
        this.dataCompra = dataCompra;
    }

    public String getGarantiaExpiracao() {
        return garantiaExpiracao;
    }

    public void setGarantiaExpiracao(String garantiaExpiracao) {
        this.garantiaExpiracao = garantiaExpiracao;
    }

    public String getStatusUso() {
        return statusUso;
    }

    public void setStatusUso(String statusUso) {
        this.statusUso = statusUso;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}