package com.example.projetoAP.Gateways;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.domain.model.Endpoint;
import com.example.projetoAP.domain.model.UpdateDTO;

import java.io.IOException;
import java.util.List;

public interface EntityRepository{
     EndpointDTO GetSalva(Endpoint e) throws IOException, InterruptedException;
     List<EndpointDTO> GetLista();
     List<EndpointDTO> GetListaDown();
     void findOndeById(UpdateDTO update) throws IOException, InterruptedException;

}
