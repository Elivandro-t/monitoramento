package com.example.projetoAP.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface RepositoryEntity extends JpaRepository<EndpointEntity,Long> {
    // campos vazios nao contam: antes mac/sn em branco batiam com outro host e barravam o cadastro
    @Query("select s from EndpointEntity s where (:mac <> '' and s.macAdress =:mac) or (:ip <> '' and s.ipv4=:ip) or (:sn <> '' and s.sn=:sn)")
    java.util.List<EndpointEntity> getReferenceByIpv4(String mac,String ip,String sn);
    @Query("select s from EndpointEntity s where s.macAdress =:mac")
    Optional<EndpointEntity> findByMacAddress(String mac);
    @Query("select s from EndpointEntity s where s.status =:n")
    Collection<EndpointEntity> findAllByStatusDown(String n);


    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE EndpointEntity e SET e.status = :status, e.ping = :ping, e.ultimaVezOnline = :ultima, e.ultimaVezOffline = :ultimaOff, e.agoraIntermitente = :agoraIntermitente WHERE e.id = :id")
    void atualizarStatus(@Param("id") Long id, @Param("status") String status,
                         @Param("ping") String ping, @Param("ultima") LocalDateTime ultima, @Param("ultimaOff") LocalDateTime ultimaOff, @Param("agoraIntermitente") boolean agoraIntermitente);

}
