package com.example.projetoAP.repository;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Service.EndpointInterfaceDTO;
import com.example.projetoAP.domain.model.Host;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.IOException;

public class SwEntity extends Host {
    public SwEntity(EndpointEntity e) throws IOException, InterruptedException {
        super(e);
    }

    public SwEntity(EndpointInterfaceDTO e,String nameGroup) throws IOException, InterruptedException {
        super(e,nameGroup);
    }

    public SwEntity(EndpointDTO e) throws IOException, InterruptedException {
        super(e);
    }
}
