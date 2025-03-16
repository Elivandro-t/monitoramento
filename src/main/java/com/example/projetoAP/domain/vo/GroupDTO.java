package com.example.projetoAP.domain.vo;

import com.example.projetoAP.domain.model.Group;

public record GroupDTO(String nome) {
    public  GroupDTO(Group s){
        this(s.getName());
    }
}
