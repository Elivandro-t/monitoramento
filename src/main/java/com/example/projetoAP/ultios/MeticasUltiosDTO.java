package com.example.projetoAP.ultios;

import com.example.projetoAP.domain.vo.MetricasDTO;

import java.util.List;
import java.util.Map;

public record MeticasUltiosDTO(String target, Map<String ,List<MetricasDTO>> result) {
}
