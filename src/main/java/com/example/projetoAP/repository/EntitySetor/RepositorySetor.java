package com.example.projetoAP.repository.EntitySetor;

import com.example.projetoAP.domain.model.Setores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RepositorySetor extends JpaRepository<EntitySetores,Long> {

    Optional<EntitySetores> getReferenceByNameComplexo(String nome);

    @Query("SELECT s FROM EntitySetores s where s.nameComplexo=:nome")
    Setores findOneNameComplexo(String nome);
}
