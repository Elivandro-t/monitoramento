package com.example.projetoAP;

import com.example.projetoAP.domain.model.Endpoint;
import com.example.projetoAP.repository.EndpointEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EndpointDTO(
        long id, String nome, String modelo,
        String sn,
        String localizacao, String mac,
        String ip, String status,
        String ping, String data, String link, String ReceiveGrupoId, Integer grupoId, String grupo, Integer filial, String nomeComplexo,
        String dataUltimaVezOnline,
        Boolean isIntermitencia,
        String pings
        ){

    public EndpointDTO(Endpoint response) {
        this(response.getId(),response.getNome()
                ,response.getModelo(),response.getSn(),
                response.getLocal(),response.getMac(),response.getIp(),response.getStatus(),response.getPing(),response.getData(),response.getLink(),"",response.getGrupoId(),response.getGrupo(),response.getFilialName(),response.getNomeCompleto(),"s",false,"");
    }

    public EndpointDTO(EndpointEntity endpointEntity) {
        this(endpointEntity.getId(),endpointEntity.getNome(),endpointEntity.model,endpointEntity.getSn(),endpointEntity.getLocal(),endpointEntity.getMac(),endpointEntity.getIp(),endpointEntity.getStatus(),endpointEntity.getPing(),endpointEntity.getData(),endpointEntity.getLink(),null,endpointEntity.getGrupoId(),endpointEntity.getGrupo(),endpointEntity.getFilialName(),endpointEntity.getNomeComplexo(),endpointEntity.ConverteHora()!=null?endpointEntity.ConverteHora():null,endpointEntity.isIntermitente(),endpointEntity.getPing());
    }


}
