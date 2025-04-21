package com.example.projetoAP.domain.model;

public record UpdateDTO(Long id,String nome, String modelo,String sn,String localizacao, String mac, String ip, String ping, String data, String link) {
}
