package com.example.projetoAP.dto;

public class MTsetorDTO {

    private Long id;
    private String nome;
    private String filial;
    private String descricao;

    public MTsetorDTO() {}

    public MTsetorDTO(Long id, String nome, String filial, String descricao) {
        this.id = id;
        this.nome = nome;
        this.filial = filial;
        this.descricao = descricao;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getFilial() { return filial; }
    public void setFilial(String filial) { this.filial = filial; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
