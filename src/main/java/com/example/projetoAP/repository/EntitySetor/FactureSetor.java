package com.example.projetoAP.repository.EntitySetor;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.projetoAP.Gateways.SetorRepositoy;
import com.example.projetoAP.domain.model.Setores;

@Component
public class FactureSetor implements SetorRepositoy {
    private final RepositorySetor repositorySetor;

    public FactureSetor(RepositorySetor repository){
        this.repositorySetor = repository;
    }

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
