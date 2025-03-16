package com.example.projetoAP.domain.vo;

import com.example.projetoAP.domain.model.Group;

public record GroupRegistroDTO(long id, String nome) {
    public GroupRegistroDTO(Group s){
        this(s.getId(),s.getName());
    }
}
