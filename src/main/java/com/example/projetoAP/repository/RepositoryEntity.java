package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface RepositoryEntity extends JpaRepository<EndpointEntity,Long> {
    @Query("select s from EndpointEntity s where s.macAdress =:mac or s.ipv4=:ip or s.sn=:sn")
    Optional<EndpointEntity> getReferenceByIpv4(String mac,String ip,String sn);
    @Query("select s from EndpointEntity s where s.macAdress =:mac")
    Optional<EndpointEntity> findByMacAddress(String mac);
    @Query("select s from EndpointEntity s where s.status =:n")
    Collection<EndpointEntity> findAllByStatusDown(String n);


    @Modifying
    @Query("UPDATE EndpointEntity e SET e.status = :status, e.ping = :ping, e.ultimaVezOnline = :ultima WHERE e.id = :id")
    void atualizarStatus(@Param("id") Long id, @Param("status") String status,
                         @Param("ping") String ping,@Param("ultima") LocalDateTime ultima);

}
