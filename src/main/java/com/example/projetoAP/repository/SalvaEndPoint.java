package com.example.projetoAP.repository;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Gateways.EntityRepository;
import com.example.projetoAP.domain.model.Endpoint;
import com.example.projetoAP.domain.model.UpdateDTO;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
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
    public EndpointDTO GetSalva(Endpoint e) throws IOException, InterruptedException {
        var op = repository.getReferenceByIpv4(e.getMac().trim(),e.getIp().trim(),e.getSn().trim());
        if(op.isPresent()){
            throw new RuntimeException("Endpoint já Existe!");
        };
         EndpointEntity entity = new EndpointEntity(e);
         EndpointEntity reponse =  repository.save(entity);
         Endpoint ew = new Endpoint(reponse);
        return new EndpointDTO(ew);
    }
    public void GetSalvas(Endpoint e) throws IOException, InterruptedException {
        var op = repository.findByMacAddress(e.getMac().trim());
        if(op.isPresent()){
           System.out.println("Endpoint existe");
           return;
        }
        EndpointEntity entity = new EndpointEntity(e);
        EndpointEntity reponse =  repository.save(entity);
         new Endpoint(reponse);
    }
    @Override
    public List<EndpointDTO> GetLista( ) {
        return repository.findAll().stream().map(EndpointDTO::new).toList();

    }
    @Override
    public List<EndpointDTO> GetListaDown() {
        return repository.findAllByStatusDown("DOWN").stream().map(EndpointDTO::new).toList();
    }

    @Override
    @Transactional
    public void findOndeById(UpdateDTO update) throws IOException, InterruptedException {
        Optional<EndpointEntity> entity = repository.findById(update.id());
        if(entity.isPresent()){
             entity.get().update(update);
             repository.save(entity.get());
        }
    }


    public EndpointDTO findOneById(Long id) {
        var entity = repository.findById(id).get();
        return new EndpointDTO(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public void atualizarStatus(Long id, String status, String ping, LocalDateTime ultima, boolean agoraIntermitente) {
        repository.atualizarStatus(id,status,ping,ultima,agoraIntermitente);
    }
}
