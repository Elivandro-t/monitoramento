package com.example.projetoAP.domain.model;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Service.EndpointInterfaceDTO;
import com.example.projetoAP.repository.EndpointEntity;
import jakarta.persistence.Version;

import java.io.IOException;

public class Endpoint extends Host{
    private long id;
   private int swgroup;

    public Endpoint(EndpointDTO e) throws IOException, InterruptedException {
        super(e);
    }

    public Endpoint(EndpointInterfaceDTO e, String nameGroup, Setores s) throws IOException, InterruptedException {
        super(e,nameGroup,s);
    }

    public Endpoint(EndpointEntity e) throws IOException, InterruptedException {
        super(e);
    }
    public Endpoint(UpdateDTO updateDTO){
        super(updateDTO);
    }


    public long getId() {
        return id;
    }

    public int getSwgroup() {
        return swgroup;
    }

}
