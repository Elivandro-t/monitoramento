package com.example.projetoAP.repository;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Gateways.EntityRepository;
import com.example.projetoAP.domain.model.Endpoin;
import com.example.projetoAP.domain.model.UpdateDTO;
import com.example.projetoAP.repository.EntitySetor.EntitySetores;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class SalvaEndPoint implements EntityRepository {
    private RepositoryEntity repository;
    private  GroupRepository groupRepository;
    public SalvaEndPoint(RepositoryEntity repository){
        this.repository = repository;
    }
    @Override
    public EndpointDTO GetSalva(Endpoin e) throws IOException, InterruptedException {
        var op = repository.getReferenceByIpv4(e.getMac().trim(),e.getIp().trim(),e.getSn().trim());
        if(op.isPresent()){
            throw new RuntimeException("Endpoint já Existe!");
        }

          EndpointEntity entity = new EndpointEntity(e);

         EndpointEntity reponse =  repository.save(entity);
         Endpoin ew = new Endpoin(reponse);
        return new EndpointDTO(ew);
    }
    public void GetSalvas(Endpoin e) throws IOException, InterruptedException {
        var op = repository.findByMacAddress(e.getMac().trim());
        if(op.isPresent()){
           System.out.println("Endpoint existe");
           return;
        }

        EndpointEntity entity = new EndpointEntity(e);

        EndpointEntity reponse =  repository.save(entity);
        Endpoin ew = new Endpoin(reponse);
    }

    @Override
    public List<EndpointDTO> GetLista( ) {
        return repository.findAll().stream().map(EndpointDTO::new).toList();

    }

    @Override
    public void findOndeById(UpdateDTO update) throws IOException, InterruptedException {
        Optional<EndpointEntity> entity = repository.findById(update.id());
        if(entity.isPresent()){
            Endpoin endpoin = new Endpoin(update);
             entity.get().update(endpoin);
             repository.save(entity.get());
        }
    }


    public EndpointDTO findOneById(Long id) {
        var entity = repository.findById(id).get();
        return new EndpointDTO(entity);
    }

}
