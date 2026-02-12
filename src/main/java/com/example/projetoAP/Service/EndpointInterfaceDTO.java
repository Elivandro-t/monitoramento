package com.example.projetoAP.Service;

import com.example.projetoAP.domain.model.Endpoint;

public record EndpointInterfaceDTO(String nome, String modelo,String sn,String localizacao, String mac, String ip, String status, String ping, String data, String link, String ReceiveGrupoId,Integer filial,String nomeComplexo,Long setorID){

    public EndpointInterfaceDTO(Endpoint response) {
        this(response.getNome()
                ,response.getModelo(),response.getSn(),
                response.getLocal(),response.getMac(),response.getIp(),response.getStatus(),response.getPing(),response.getData(),response.getLink(),"response",response.getFilialName(),String.valueOf(response.getGrupoSetor()), response.getGrupoSetor());
    }


}
