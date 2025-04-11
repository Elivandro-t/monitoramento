package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MetricasRepository extends JpaRepository<MetricasEntity,Long> {
    @Query("SELECT p FROM MetricasEntity p where p.itemid=:id and p.hostid=:hostid ORDER BY p.id DESC")
    List<MetricasEntity> findAllById(long id,long hostid);
}
