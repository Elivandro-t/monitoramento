package com.example.projetoAP.domain.model;

import com.example.projetoAP.domain.vo.LostDTO;
import com.example.projetoAP.domain.vo.MetricasDTO;
import com.example.projetoAP.repository.MetricasEntity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Timer;

public class Metricas {
    private long id;
    private long hostid;
    private long itemid;
    private String icmpName;
    private long clock;
    private String value;
    private String ns;
    public Metricas(MetricasDTO metricasDTO){
        this.value = String.valueOf(metricasDTO.value());
        this.hostid = metricasDTO.hostId();
        this.itemid = metricasDTO.itemId();
        this.clock = Instant.now().getEpochSecond();
        this.icmpName = metricasDTO.icmpName();
    }
    public Metricas(LostDTO metricasDTO){
        this.value = String.valueOf(metricasDTO.value());
        this.hostid = metricasDTO.hostId();
        this.itemid = metricasDTO.itemId();
        this.clock = Instant.now().getEpochSecond();
        this.icmpName = metricasDTO.icmpName();
    }
    public Metricas(){}

    public Metricas(MetricasEntity metricasDTO) {
        this.value = metricasDTO.getValue();
        this.hostid = metricasDTO.getHostid();
        this.itemid = metricasDTO.getItemid();
        this.clock = Instant.now().getEpochSecond();
        this.icmpName = metricasDTO.getIcmpName();
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
}
