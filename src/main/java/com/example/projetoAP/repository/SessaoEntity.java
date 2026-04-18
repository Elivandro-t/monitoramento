package com.example.projetoAP.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessao", indexes = {
        @Index(name = "idx_sessao_usuario", columnList = "usuario"),
        @Index(name = "idx_sessao_ativa", columnList = "ativa")
})
public class SessaoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String usuario;
    private String token;
    private LocalDateTime horaEntrada;
    private LocalDateTime horaSaida;
    private LocalDateTime horaExpiracao;

    @Column(nullable = false)
    private boolean ativa = true;

    private String ipMaquina;
    private String navegador;

    public SessaoEntity() {}

    public SessaoEntity(String usuario, String token, String ipMaquina, String navegador) {
        this.usuario = usuario;
        this.token = token;
        this.horaEntrada = LocalDateTime.now();
        this.horaExpiracao = LocalDateTime.now().plusHours(5);
        this.ativa = true;
        this.ipMaquina = ipMaquina;
        this.navegador = navegador;
    }

    public void encerrar() {
        this.ativa = false;
        this.horaSaida = LocalDateTime.now();
    }

    public void expirar() {
        this.ativa = false;
        this.horaSaida = LocalDateTime.now();
    }

    public long getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getToken() { return token; }
    public LocalDateTime getHoraEntrada() { return horaEntrada; }
    public LocalDateTime getHoraSaida() { return horaSaida; }
    public LocalDateTime getHoraExpiracao() { return horaExpiracao; }
    public boolean isAtiva() { return ativa; }
    public String getIpMaquina() { return ipMaquina; }
    public String getNavegador() { return navegador; }
}
