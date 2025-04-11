package com.example.projetoAP.repository.EntitySetor;

import com.example.projetoAP.Gateways.SetorRepositoy;
import com.example.projetoAP.domain.model.Setor.SetorDTO;
import com.example.projetoAP.domain.model.Setores;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FactureSetor implements SetorRepositoy {
    RepositorySetor repositorySetor;
    public FactureSetor(RepositorySetor repository){
        this.repositorySetor = repository;
    }
    public FactureSetor(){}
    @Override
    public void setRegistro(Setores s) {
        var buscados = repositorySetor.getReferenceByNameComplexo(s.getNameComplexo());
        if(buscados.isPresent()){
            throw new RuntimeException("Nome já existe");

        }else{
            EntitySetores saveResponse = new EntitySetores(s);
            repositorySetor.save(saveResponse);
        }
    }

    @Override
    public List<Setores> getLista() {
        return repositorySetor.findAll().stream().map(Setores::new).toList();
    }
    public Setores findOne(String nome) {
        return repositorySetor.findOneNameComplexo(nome);
    }
}
