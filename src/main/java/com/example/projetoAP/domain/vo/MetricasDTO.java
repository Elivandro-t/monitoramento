package com.example.projetoAP.domain.vo;

import com.example.projetoAP.domain.model.Metricas;
import com.example.projetoAP.repository.MetricasEntity;

import java.util.Optional;

public record MetricasDTO(String value,long itemId, long hostId,String icmpName,Long clock) {
    public MetricasDTO(MetricasEntity e) {
        this(e.getValue(),e.getItemid(),e.getHostid(),e.getIcmpName(),e.getClock());
    }


}
