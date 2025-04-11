package com.example.projetoAP.domain.model;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Service.EndpointInterfaceDTO;
import com.example.projetoAP.repository.EndpointEntity;
import com.example.projetoAP.repository.EntitySetor.EntitySetores;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Endpoin extends Host{
    private long id;
   private int swgroup;

    public Endpoin(EndpointDTO e) throws IOException, InterruptedException {
        super(e);
    }

    public Endpoin(EndpointInterfaceDTO e,String nameGroup,Setores s) throws IOException, InterruptedException {
        super(e,nameGroup,s);
    }

    public Endpoin(EndpointEntity e) throws IOException, InterruptedException {
        super(e);
    }


    public long getId() {
        return id;
    }

    public int getSwgroup() {
        return swgroup;
    }

}
