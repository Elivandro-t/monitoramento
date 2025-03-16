package com.example.projetoAP.Gateways;

import com.example.projetoAP.domain.model.Group;

import java.util.List;
import java.util.Set;

public interface GroupInterfaceRepository{
     void save(Group s);
    List<Group> findAll();
    Group findOnde(long id);

}
