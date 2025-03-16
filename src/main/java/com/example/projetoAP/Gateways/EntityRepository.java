package com.example.projetoAP.Gateways;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.domain.model.Endpoin;

import java.io.IOException;
import java.util.List;

public interface EntityRepository{
    public EndpointDTO GetSalva(Endpoin e) throws IOException, InterruptedException;
    public List<EndpointDTO> GetLista();

}
