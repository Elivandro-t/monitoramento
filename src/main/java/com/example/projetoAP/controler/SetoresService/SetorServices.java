package com.example.projetoAP.controler.SetoresService;

import com.example.projetoAP.domain.model.Setor.SetorDTO;
import com.example.projetoAP.domain.model.Setor.SetorRegistroDTO;

import java.util.List;

public interface SetorServices {
    void setRegistro(SetorRegistroDTO s);
    List<SetorDTO> getLista();

}
