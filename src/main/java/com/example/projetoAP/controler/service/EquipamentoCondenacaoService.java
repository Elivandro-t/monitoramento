package com.example.projetoAP.controler.service;

import com.example.projetoAP.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EquipamentoCondenacaoService {

    private final EquipamentoCondenadoRepository repository;
    private final AgenteRepository agenteRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public EquipamentoCondenacaoService(EquipamentoCondenadoRepository repository,
                                        AgenteRepository agenteRepository,
                                        ManutencaoRepository manutencaoRepository) {
        this.repository = repository;
        this.agenteRepository = agenteRepository;
        this.manutencaoRepository = manutencaoRepository;
    }

    /**
     * Snapshota o agente e suas manutencoes em EquipamentoCondenadoEntity,
     * remove as manutencoes originais e o proprio agente.
     * Preserva a manutencao que disparou a condenacao apenas no snapshot (campo historicoManutencoes).
     */
    @Transactional
    public EquipamentoCondenadoEntity condenar(AgenteEntity ag, String motivo,
                                               String laudoFinalTextual, String condenadoPor) {
        if (repository.existsByAgentId(ag.getAgentId())) {
            throw new IllegalStateException("Equipamento ja condenado: " + ag.getAgentId());
        }

        var manuts = manutencaoRepository.findByAgentIdOrderByDataManutencaoDesc(ag.getAgentId());
        List<Map<String, Object>> historico = manuts.stream().map(m -> {
            Map<String, Object> r = new HashMap<>();
            r.put("id", m.getId());
            r.put("tecnico", (m.getTecnicoNome() == null ? "" : m.getTecnicoNome())
                    + " (" + (m.getTecnicoUsername() == null ? "" : m.getTecnicoUsername()) + ")");
            r.put("data", m.getDataManutencao() != null ?
                    m.getDataManutencao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
            r.put("descricao", m.getDescricao());
            r.put("laudo", m.getLaudoFinal());
            r.put("status", m.getStatus());
            r.put("tipoManutencao", m.getTipoManutencao());
            r.put("anexo", m.getAnexoOriginal());
            return r;
        }).toList();

        String historicoJson;
        try { historicoJson = mapper.writeValueAsString(historico); }
        catch (Exception e) { historicoJson = "[]"; }

        String ultimoLaudo = manuts.stream().map(ManutencaoEntity::getLaudoFinal)
                .filter(l -> l != null && !l.isBlank()).findFirst().orElse("");

        EquipamentoCondenadoEntity cond = new EquipamentoCondenadoEntity();
        cond.setAgentId(ag.getAgentId());
        cond.setHostname(ag.getHostname());
        cond.setPatrimonio(ag.getPatrimonio());
        cond.setMarca(ag.getMarca());
        cond.setModelo(ag.getModelo());
        cond.setNumeroSerie(ag.getNumeroSerie());
        cond.setSetor(ag.getSetor());
        cond.setFilial(ag.getFilial());
        cond.setTipo(ag.getTipo());
        cond.setMotivoCondenacao(motivo == null ? "" : motivo);
        cond.setLaudoFinal(laudoFinalTextual == null || laudoFinalTextual.isBlank()
                ? ultimoLaudo : laudoFinalTextual);
        cond.setHistoricoManutencoes(historicoJson);
        cond.setTotalManutencoes(ag.getNumManutencoes());
        cond.setCondenadoPor(condenadoPor);
        cond.setDataCondenacao(LocalDateTime.now());
        repository.save(cond);

        if (!manuts.isEmpty()) manutencaoRepository.deleteAll(manuts);
        agenteRepository.delete(ag);

        return cond;
    }
}
