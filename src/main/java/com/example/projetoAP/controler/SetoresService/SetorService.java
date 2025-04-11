package com.example.projetoAP.controler.SetoresService;

import com.example.projetoAP.domain.model.Setor.SetorDTO;
import com.example.projetoAP.domain.model.Setor.SetorRegistroDTO;
import com.example.projetoAP.domain.model.Setores;
import com.example.projetoAP.repository.EntitySetor.FactureSetor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SetorService implements SetorServices{
    FactureSetor setor;
    public SetorService(FactureSetor setor){
        this.setor = setor;
    }
    public SetorService(){}

    @Override
    public void setRegistro(SetorRegistroDTO s) {
           if(s!=null){
               setor.setRegistro(new Setores(s));
           }
    }

    @Override
    public List<SetorDTO> getLista() {
        return setor.getLista().stream().map(e-> new SetorDTO(e.getId(),e.getNameComplexo(),String.valueOf(e.getFilial()))).toList();
    }
}
