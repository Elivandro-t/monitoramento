package com.example.projetoAP.domain.model;

import com.example.projetoAP.domain.vo.GroupDTO;
import com.example.projetoAP.repository.GroupEntity;

public class Group {
    private long id;
    private String name;
    public Group(GroupDTO group){
        this.name = group.nome();
    }
    public Group(GroupEntity group){
        this.id = group.getId();
        this.name = group.getName();
    }
    public Group(){
    }
    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
}
