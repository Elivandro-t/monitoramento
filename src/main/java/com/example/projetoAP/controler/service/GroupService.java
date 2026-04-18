package com.example.projetoAP.controler.service;

import com.example.projetoAP.domain.model.Group;
import com.example.projetoAP.domain.vo.GroupDTO;
import com.example.projetoAP.domain.vo.GroupRegistroDTO;
import com.example.projetoAP.repository.FactureGroup;
import com.example.projetoAP.repository.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {
    FactureGroup factureGroup;
    GroupRepository groupRepository;

    public GroupService(FactureGroup factureGroup, GroupRepository groupRepository) {
        this.factureGroup = factureGroup;
        this.groupRepository = groupRepository;
    }

    public void salve(GroupDTO groupDTO) {
        var n = new Group(groupDTO);
        factureGroup.save(n);
    }

    public List<GroupRegistroDTO> findAll() {
        return factureGroup.findAll().stream().map(GroupRegistroDTO::new).toList();
    }

    public GroupDTO findOne(long id) {
        Group group = factureGroup.findOnde(id);
        return new GroupDTO(group);
    }

    @Transactional
    public void deletar(Long id) {
        groupRepository.deleteById(id);
    }
}
