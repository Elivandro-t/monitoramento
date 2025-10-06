package com.example.projetoAP.ultios;

import com.example.projetoAP.domain.vo.GroupRegistroDTO;

public record GroupAPIDTO(long groupid, String name) {
    public GroupAPIDTO(GroupRegistroDTO e) {
        this(e.id(), e.nome());
    }
}
