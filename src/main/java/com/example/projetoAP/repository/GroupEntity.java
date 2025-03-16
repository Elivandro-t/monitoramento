package com.example.projetoAP.repository;

import com.example.projetoAP.domain.model.Group;
import com.example.projetoAP.domain.vo.GroupDTO;
import jakarta.persistence.*;

@Entity
@Table(name="group_tamplete")
public class GroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    public  GroupEntity(){}
    public GroupEntity(Group group){
        this.name = group.getName();
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(long id) {
        this.id = id;
    }
}
