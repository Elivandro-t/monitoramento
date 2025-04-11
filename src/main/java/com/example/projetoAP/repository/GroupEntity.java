package com.example.projetoAP.repository;

import com.example.projetoAP.domain.model.Group;
import com.example.projetoAP.domain.vo.GroupDTO;
import jakarta.persistence.*;

@Entity
@Table(name="hstgrp")
public class GroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "groupid", nullable = true, updatable = true)
    private long groupid;
    private String name;
    private int internal;
    private int flags;
    public  GroupEntity(){}
    public GroupEntity(Group group){

        this.name = group.getName();
        this.flags = 0;
        this.internal = 0;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return groupid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(long id) {
        this.groupid = id;
    }

    public int getInternal() {
        return internal;
    }

    public int getFlags() {
        return flags;
    }
}
