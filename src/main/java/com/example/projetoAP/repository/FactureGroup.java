package com.example.projetoAP.repository;

import com.example.projetoAP.Gateways.GroupInterfaceRepository;
import com.example.projetoAP.domain.model.Group;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FactureGroup implements GroupInterfaceRepository {
    GroupRepository groupRepository;
    FactureGroup(GroupRepository groupRepository){
        this.groupRepository = groupRepository;
    }
    @Override
    public void save(Group s) {
       GroupEntity grupo = new GroupEntity(s);
       System.out.println("chenaod aqui "+grupo.getName());
       groupRepository.save(grupo);
    }

    @Override
    public List<Group> findAll() {
        return groupRepository.findAll()
                .stream().map(Group::new)
                .toList();
    }

    @Override
    public Group findOnde(long id) {
        var lista  = groupRepository.getReferenceById(id);
        return new Group(lista);
    }
}
