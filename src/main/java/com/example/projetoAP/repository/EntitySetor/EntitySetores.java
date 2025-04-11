package com.example.projetoAP.repository.EntitySetor;

import com.example.projetoAP.domain.model.Setor.SetorDTO;
import com.example.projetoAP.domain.model.Setores;
import jakarta.persistence.*;

@Entity
@Table(name = "setor")
public class EntitySetores {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private  String nameComplexo;
    private Integer filial;
    public  EntitySetores(){};
    public EntitySetores(Setores s) {
        this.nameComplexo = s.getNameComplexo();
        this.filial = s.getFilial();
        this.id = s.getId();
    }

    public long getId() {
        return id;
    }

    public Integer getFilial() {
        return filial;
    }

    public String getNameComplexo() {
        return nameComplexo;
    }
}
