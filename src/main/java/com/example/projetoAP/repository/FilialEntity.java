package com.example.projetoAP.repository;

import jakarta.persistence.*;

@Entity
@Table(name = "filial")
public class FilialEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String nome;

    private String codigo;
    private String cidade;
    private String estado;

    public FilialEntity() {}

    public FilialEntity(String nome, String codigo, String cidade, String estado) {
        this.nome = nome;
        this.codigo = codigo;
        this.cidade = cidade;
        this.estado = estado;
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getCodigo() { return codigo; }
    public String getCidade() { return cidade; }
    public String getEstado() { return estado; }

    public void setNome(String nome) { this.nome = nome; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public void setEstado(String estado) { this.estado = estado; }
}
