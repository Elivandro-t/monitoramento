package com.example.projetoAP.domain.model.SW;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Service.EndpointInterfaceDTO;
import com.example.projetoAP.domain.model.Host;
import com.example.projetoAP.repository.EndpointEntity;

import java.io.IOException;

public class SW extends Host {
    private long id;
    public SW(EndpointDTO e) throws IOException, InterruptedException {
        super(e);
    }

    public SW(EndpointEntity e) throws IOException, InterruptedException {
        super(e);
    }

    public SW(EndpointInterfaceDTO e,String nameGroup) throws IOException, InterruptedException {
        super(e,nameGroup);
    }
}
