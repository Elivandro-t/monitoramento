package com.example.projetoAP.Gateways;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.domain.model.Endpoin;
import com.example.projetoAP.domain.model.UpdateDTO;
import com.example.projetoAP.repository.EntitySetor.EntitySetores;
import com.example.projetoAP.repository.GroupEntity;

import java.io.IOException;
import java.util.List;

public interface EntityRepository{
     EndpointDTO GetSalva(Endpoin e) throws IOException, InterruptedException;
     List<EndpointDTO> GetLista();
     void findOndeById(UpdateDTO update) throws IOException, InterruptedException;

}
