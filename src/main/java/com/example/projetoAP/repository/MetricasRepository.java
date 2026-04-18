package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MetricasRepository extends JpaRepository<MetricasEntity,Long> {
    @Query("SELECT p FROM MetricasEntity p where p.itemid=:id and p.hostid=:hostid ORDER BY p.id DESC")
    List<MetricasEntity> findAllById(long id,long hostid);
    @Query("SELECT p FROM MetricasEntity p where p.hostid=:hostid ORDER BY p.id DESC")
    List<MetricasEntity> findAllByIdOne(Long hostid);

    @Query("SELECT p FROM MetricasEntity p where p.hostid=:hostid AND p.timestamp >= :desde ORDER BY p.id DESC")
    List<MetricasEntity> findAllByIdOneAndTimestampAfter(Long hostid, LocalDateTime desde);

    List<MetricasEntity> findTop5000ByHostidOrderByTimestampDesc(Long hostid);
    List<MetricasEntity> findTop5000ByHostidAndTimestampAfterOrderByTimestampDesc(Long hostid, LocalDateTime desde);
    List<MetricasEntity> findTop2000ByItemidAndHostidOrderByIdDesc(long itemid, long hostid);

    List<MetricasEntity> findTop200000ByHostidAndTimestampAfterOrderByTimestampDesc(Long hostid, LocalDateTime desde);

    List<MetricasEntity> findAllByTimestampBefore(LocalDateTime timeDay);
}
