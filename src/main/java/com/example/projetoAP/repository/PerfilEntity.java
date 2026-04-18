package com.example.projetoAP.repository;

import jakarta.persistence.*;

@Entity
@Table(name = "perfil")
public class PerfilEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, nullable = false)
    private String nome;

    @Column(length = 500)
    private String descricao;

    private boolean acessoUsuarios = false;
    private boolean acessoHistorico = false;
    private boolean acessoSetores = false;
    private boolean acessoRegistro = false;
    private boolean acessoDelete = false;

    public PerfilEntity() {}

    public PerfilEntity(String nome, String descricao,
                        boolean acessoUsuarios, boolean acessoHistorico,
                        boolean acessoSetores, boolean acessoRegistro,
                        boolean acessoDelete) {
        this.nome = nome;
        this.descricao = descricao;
        this.acessoUsuarios = acessoUsuarios;
        this.acessoHistorico = acessoHistorico;
        this.acessoSetores = acessoSetores;
        this.acessoRegistro = acessoRegistro;
        this.acessoDelete = acessoDelete;
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public boolean isAcessoUsuarios() { return acessoUsuarios; }
    public boolean isAcessoHistorico() { return acessoHistorico; }
    public boolean isAcessoSetores() { return acessoSetores; }
    public boolean isAcessoRegistro() { return acessoRegistro; }
    public boolean isAcessoDelete() { return acessoDelete; }

    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setAcessoUsuarios(boolean acessoUsuarios) { this.acessoUsuarios = acessoUsuarios; }
    public void setAcessoHistorico(boolean acessoHistorico) { this.acessoHistorico = acessoHistorico; }
    public void setAcessoSetores(boolean acessoSetores) { this.acessoSetores = acessoSetores; }
    public void setAcessoRegistro(boolean acessoRegistro) { this.acessoRegistro = acessoRegistro; }
    public void setAcessoDelete(boolean acessoDelete) { this.acessoDelete = acessoDelete; }
}
