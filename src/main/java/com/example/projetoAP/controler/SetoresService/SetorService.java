package com.example.projetoAP.controler.SetoresService;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.projetoAP.domain.model.Setor.SetorDTO;
import com.example.projetoAP.domain.model.Setor.SetorRegistroDTO;
import com.example.projetoAP.domain.model.Setores;
import com.example.projetoAP.repository.EntitySetor.FactureSetor;
import com.example.projetoAP.repository.EntitySetor.RepositorySetor;

@Service
public class SetorService implements SetorServices {
    private final FactureSetor setor;
    private final RepositorySetor repositorySetor;

    public SetorService(FactureSetor setor, RepositorySetor repositorySetor) {
        this.setor = setor;
        this.repositorySetor = repositorySetor;
    }

    @Override
    public void setRegistro(SetorRegistroDTO s) {
        if (s != null) {
            setor.setRegistro(new Setores(s));
        }
    }

    @Override
    public List<SetorDTO> getLista() {
        return setor.getLista().stream()
                .map(e -> new SetorDTO(e.getId(), e.getNameComplexo(), String.valueOf(e.getFilial())))
                .toList();
    }

    @Transactional
    public void deletar(Long id) {
        repositorySetor.deleteById(id);
    }
}
