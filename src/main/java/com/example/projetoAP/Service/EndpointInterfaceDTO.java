package com.example.projetoAP.Service;

import com.example.projetoAP.domain.model.Endpoin;
import com.example.projetoAP.repository.EndpointEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public record EndpointInterfaceDTO(String nome, String modelo,String sn,String localizacao, String mac, String ip, String status, String ping, String data, String link, String ReceiveGrupoId,Integer filial,String nomeComplexo,Long setorID){

    public EndpointInterfaceDTO(Endpoin response) {
        this(response.getNome()
                ,response.getModelo(),response.getSn(),
                response.getLocal(),response.getMac(),response.getIp(),response.getStatus(),response.getPing(),response.getData(),response.getLink(),"response",response.getFilialName(),String.valueOf(response.getGrupoSetor()), response.getGrupoSetor());
    }


}
