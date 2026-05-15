package com.example.projetoAP.repository;

import com.example.projetoAP.domain.model.Endpoint;
import com.example.projetoAP.domain.model.UpdateDTO;
import jakarta.persistence.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(name="endpoint")
public class EndpointEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String hostname;
    public String model;
    private String macAdress;
    private String profile;
    private String ipv4;
    private String status;
    private String ping;
    private String link;
    private String data;
    private String sn;
    private Integer grupoId;
    private String grupo;
    private long grupoSetor;
    private Integer filialName;
    private String NomeComplexo;
    private LocalDateTime ultimaVezOnline;
    private boolean agoraIntermitente;
    public EndpointEntity(){}
    public EndpointEntity(Endpoint e){
        this.hostname = e.getNome();
        this.model = e.getModelo();
        this.macAdress = e.getMac();
        this.profile = e.getLocal();
        this.ipv4 = e.getIp();
        this.status = e.getStatus();
        this.ping = e.getPing();
        this.link = e.getLink();
        this.data = e.getData();
        this.grupo = e.getGrupo();
        this.grupoId = e.getGrupoId();
        this.sn = e.getSn();
        this.filialName = e.getFilialName();
        this.grupoSetor = e.getGrupoSetor();
        this.NomeComplexo = e.getNomeCompleto();
    }

    public String getNome() {
        return hostname;
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
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        return macAdress;
    }

    public void setMac(String mac) {
        this.macAdress = mac;
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

    public long getId() {
        return id;
    }

//    public String getCmd(String  host) throws IOException, InterruptedException {
//        String linha = null;
//        String os = System.getProperty("os.name").toLowerCase();
//        String pingCmd = os.contains("win") ? "ping -n 4 " + host : "ping -c 4 " + host;
//        Process process = Runtime.getRuntime().exec(pingCmd);
//        StringBuilder result = new StringBuilder();
//        String line;
//        Pattern pattern = Pattern.compile("icmp_seq=(\\d+) ttl=(\\d+) time=([\\d.]+) ms");
//        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//        while ((line = bufferedReader.readLine())!=null){
//            Matcher matcher = pattern.matcher(line);
//            if (matcher.find()) {
//                String icmpSeq = matcher.group(1);
//                String ttl = matcher.group(2);
//                String time = matcher.group(3);
//                result.append("ICMP Seq: ").append(icmpSeq)
//                        .append(", TTL: ").append(ttl)
//                        .append(", Tempo: ").append(time).append(" ms\n");
//            }
//        }
//        process.waitFor();
//        return result.toString().trim();
//    }
//    public String statusIp(String host) throws IOException {
////        InetAddress inet = InetAddress.getByName(host);
////        boolean isReactive = inet.isReachable(300);
//        Process p = Runtime.getRuntime().exec("ping -c 1 " + host); // No Windows, use "ping -n 1"
//        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
//        String line;
//        boolean isReactive = false;
//        while ((line = input.readLine()) != null) {
//            if (line.contains("ttl=") || line.contains("TTL=")) {
//                isReactive = true;
//                break;
//            }
//        }
//        input.close();
//        return isReactive ? "UP":"DOWN";
//    }
//    public  void atualiza(String ip) throws IOException, InterruptedException {
//        this.ping =getCmd(ip);
//        this.Status =statusIp(ip);
//       if("DOWN".contains(statusIp(ip))){
//            this.ultimaVezOnline = LocalDateTime.now();
//           return;
//      }
//      this.ultimaVezOnline = null;
//
//    }
    public  void update(Endpoint update) throws IOException, InterruptedException {
        this.hostname = update.getNome();
        this.profile = update.getLocal();
        this.link = update.getLink();
        this.macAdress = update.getMac();
        this.ipv4 = update.getIp();
        this.model = update.model;

    }
    public Integer getGrupoId() {
        return grupoId;
    }

    public String getGrupo() {
        return grupo;
    }

    public String getSn() {
        return sn;
    }

    public Integer getFilialName() {
        return filialName;
    }

    public long getGrupoSetor() {
        return grupoSetor;
    }

    public String getNomeComplexo() {
        return null;
    }

    public void atualizaEndpoint(String ip) {
    }
    public String getHostname() {
        return hostname;
    }
    public LocalDateTime getUltimaVezOnline() {
        return ultimaVezOnline;
    }

    public void setUltimaVezOnline(LocalDateTime ultimaVezOnline) {
        this.ultimaVezOnline = ultimaVezOnline;
    }
    public String ConverteHora(){
        if(ultimaVezOnline!=null){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String formatado = ultimaVezOnline.format(formatter);
            return  formatado;
        }
        return null;
    }

    public void update(UpdateDTO update) {
        this.hostname = update.nome();
        this.macAdress = update.mac();
        this.ipv4 = update.ip();
        this.profile = update.localizacao();
        this.model = update.modelo();
        this.sn = update.sn();
    }

    public boolean isIntermitente() {
        return agoraIntermitente;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setIntermitente(boolean intermitente) {
        this.agoraIntermitente = intermitente;
    }
}
