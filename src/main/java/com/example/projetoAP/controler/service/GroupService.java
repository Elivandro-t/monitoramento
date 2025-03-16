package com.example.projetoAP.controler.service;
import com.example.projetoAP.domain.model.Group;
import com.example.projetoAP.domain.vo.GroupDTO;
import com.example.projetoAP.domain.vo.GroupRegistroDTO;
import com.example.projetoAP.repository.FactureGroup;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupService {
    FactureGroup factureGroup;
    public  GroupService(FactureGroup factureGroup){
        this.factureGroup = factureGroup;
    }
   public void salve(GroupDTO groupDTO){
        var n = new Group(groupDTO);
        System.out.println("deu "+n.getName());
        factureGroup.save(n);
   }
   public List<GroupRegistroDTO> findAll(){
        var lista = factureGroup.findAll().stream().map(GroupRegistroDTO::new).toList();
        return lista;
   }
    public GroupDTO findOne(long id){
        System.out.println("meu id"+id);
     Group group = factureGroup.findOnde(id);
     return  new GroupDTO(group);

    }

}
