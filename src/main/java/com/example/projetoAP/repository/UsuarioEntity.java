package com.example.projetoAP.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String senha;

    private String nome;
    private String filial;

    @Column(nullable = false)
    private String perfil = "INFORMATICA";

    private String setor;
    private String cargo;

    @Column(nullable = false)
    private boolean autorizado = true;

    private LocalDateTime ultimoLogin;

    public UsuarioEntity() {}

    public UsuarioEntity(String username, String senha) {
        this.username = username;
        this.senha = senha;
        this.perfil = "ADMIN";
        this.autorizado = true;
    }

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getSenha() { return senha; }
    public String getNome() { return nome; }
    public String getFilial() { return filial; }
    public String getPerfil() { return perfil; }
    public String getSetor() { return setor; }
    public String getCargo() { return cargo; }
    public boolean isAutorizado() { return autorizado; }
    public LocalDateTime getUltimoLogin() { return ultimoLogin; }

    public void setUltimoLogin(LocalDateTime ultimoLogin) { this.ultimoLogin = ultimoLogin; }
    public void setNome(String nome) { this.nome = nome; }
    public void setFilial(String filial) { this.filial = filial; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
    public void setSetor(String setor) { this.setor = setor; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public void setAutorizado(boolean autorizado) { this.autorizado = autorizado; }
    public void setSenha(String senha) { this.senha = senha; }
    public void setUsername(String username) { this.username = username; }
}
