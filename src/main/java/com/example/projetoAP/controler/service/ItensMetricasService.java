package com.example.projetoAP.controler.service;

import com.example.projetoAP.domain.vo.IcmpDTO;
import com.example.projetoAP.repository.ICMPRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItensMetricasService {
    ICMPRepository icmpRepository;
    public  ItensMetricasService(ICMPRepository icmpRepository){
        this.icmpRepository = icmpRepository;
    }
    public Map<String, List<IcmpDTO>> lista(){
        List<IcmpDTO> lista = icmpRepository.findAll().stream().map(item->new IcmpDTO((item))).toList();
        Map<String,List<IcmpDTO>> listMap = new HashMap<>();
        listMap.put("item",lista);
        return listMap;

    }

}
