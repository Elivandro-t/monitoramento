package com.example.projetoAP.domain.model;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Service.EndpointInterfaceDTO;
import com.example.projetoAP.repository.EndpointEntity;

import java.io.IOException;
import java.net.InetAddress;
public class Host {
    private String hostname;
    public String model;
    private String mac_adress;
    private String profile;
    private String ipv4;
    private String Status;
    private String ping;
    private String link;
    private String data;
    private String sn;
    private Integer grupoId;
    private String grupo;
    public Host(EndpointDTO e) throws IOException, InterruptedException {
        this.hostname = e.nome();
        this.model = e.modelo();
        this.mac_adress = e.mac();
        this.profile = e.localizacao();
        this.ipv4 = e.ip();
        this.Status = statusIp(e.ip());
        this.data = e.data();
        this.link = e.link();
        this.grupo = e.grupo();
        this.grupoId = Integer.parseInt(e.ReceiveGrupoId());
        this.sn = e.sn();

    }
    public Host(EndpointInterfaceDTO e,String s) throws IOException, InterruptedException {
        this.hostname = e.nome();
        this.model = e.modelo();
        this.mac_adress = e.mac();
        this.profile = e.localizacao();
        this.ipv4 = e.ip();
        this.Status = statusIp(e.ip());
        this.data = e.data();
        this.link = e.link();
        this.grupo = s;
        this.ping = "";
        this.grupoId = Integer.parseInt(e.ReceiveGrupoId());
        this.sn = e.sn();

    }
    public Host(EndpointEntity e) throws IOException, InterruptedException {
        this.hostname = e.getNome();
        this.model = e.getModelo();
        this.mac_adress = e.getMac();
        this.profile = e.getLocal();
        this.ipv4 = e.getIp();
        this.grupo = e.getGrupo();
        this.grupoId = e.getGrupoId();
        this.Status = statusIp(e.getIp());
        this.data = e.getIp();
        this.link = e.getLink();
        this.sn = e.getSn();


    }

    public String getNome() {
        return hostname;
    }

    private String statusIp(String host) throws IOException {
        InetAddress inet = InetAddress.getByName(host);
        boolean isReactive = inet.isReachable(3000);
        return isReactive ? "ON":"DOWN";
    }

    public void setNome(String nome) {
        this.hostname = nome;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getPing() {
        return ping;
    }

    public void setPing(String ping) {
        this.ping = ping;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public String getIp() {
        return ipv4;
    }

    public void setIp(String ip) {
        this.ipv4 = ip;
    }

    public String getLocal() {
        return profile;
    }

    public void setLocal(String local) {
        this.profile = local;
    }

    public String getMac() {
        return mac_adress;
    }

    public void setMac(String mac) {
        this.mac_adress = mac;
    }

    public String getModelo() {
        return model;
    }

    public void setModelo(String modelo) {
        this.model = modelo;
    }

    public String getData() {
        return data;
    }

    public Integer getGrupoId() {
        return grupoId;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public String getSn() {
        return sn;
    }
}
