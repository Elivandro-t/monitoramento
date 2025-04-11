package com.example.projetoAP.Gateways;

import com.example.projetoAP.domain.model.Setor.SetorDTO;
import com.example.projetoAP.domain.model.Setores;

import java.util.List;

public interface SetorRepositoy {
    void setRegistro(Setores s);
    List<Setores> getLista();

}
