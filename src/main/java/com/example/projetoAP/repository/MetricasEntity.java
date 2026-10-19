package com.example.projetoAP.repository;

import com.example.projetoAP.domain.model.Metricas;
import com.example.projetoAP.domain.vo.MetricasDTO;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Entity
@Table(name = "history", indexes = {
        @Index(name = "idx_history_hostid_timestamp", columnList = "hostid,timestamp"),
        @Index(name = "idx_history_hostid_itemid", columnList = "hostid,itemid"),
        @Index(name = "idx_history_timestamp", columnList = "timestamp")
})
public class MetricasEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long hostid;
    private long itemid;
    private String icmpName;
    private long clock;
    private String value;
    private String ns;
    private LocalDateTime timestamp;
    public MetricasEntity(){}
    public MetricasEntity(Metricas metricas){
        this.value = metricas.getValue();
         this.hostid = metricas.getHostid();
         this.itemid = metricas.getItemid();
        this.clock = metricas.getClock();
        this.icmpName = metricas.getIcmpName();
        this.timestamp = LocalDateTime.now();
    }
    public String unit( ){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm:ss", Locale.forLanguageTag("pt-BR"));
        String formatString = timestamp.format(formatter);
        return formatString;

    }
    public long getItemid() {
        return itemid;
    }

    public String getNs() {
        return ns;
    }

    public String getValue() {
        return value;
    }

    public long getClock() {
        return clock;
    }

    public long getId() {
        return id;
    }

    public long getHostid() {
        return hostid;
    }

    public String getIcmpName() {
        return icmpName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
