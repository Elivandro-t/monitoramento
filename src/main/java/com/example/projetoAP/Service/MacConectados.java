package com.example.projetoAP.Service;

import com.example.projetoAP.EndpointDTO;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Component
public class MacConectados {

    public List<EndpointDTO> getConectados(Snmp snmp, CommunityTarget communityTarget) throws Exception {
        Map<String,String > macT = new HashMap<>();
        List<EndpointDTO> listaEndpoint = new ArrayList<>();
        String apiAp = null;
        OID macTableOID = new OID("1.3.6.1.2.1.17.4.3.1.1"); // MACs conectados
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(macTableOID));
        pdu.setType(PDU.GETNEXT);
        pdu.setType(PDU.GET);

        OID InforAp = new OID("1.3.6.1.2.1.1.5.0");
        PDU InforApPud = new PDU();
        InforApPud.add(new VariableBinding(InforAp));
        InforApPud.setType(PDU.GETNEXT);// MACs conectados


        ResponseEvent responseAp = snmp.get(pdu, communityTarget);
        boolean finished = false;
        while (!finished) {
            ResponseEvent response = snmp.getNext(pdu, communityTarget);
            ResponseEvent responseAP = snmp.get(InforApPud, communityTarget);
            if (response != null && response.getResponse() != null) {
                PDU responsePDU = response.getResponse();
                VariableBinding vb = responsePDU.get(0);
                 System.out.println("Nome da minha ap "+ responseAP.getResponse());
                System.out.println("Cidade "+apiAp);
                if (!vb.getOid().startsWith(macTableOID)) {
                    finished = true;
                } else {
                    String mac = formatMacAddress((OctetString) vb.getVariable());
                    macT.put(mac,"Desconhecido" );
                    pdu.clear();
                    pdu.add(new VariableBinding(vb.getOid())); // Próximo OID

                }
            } else {
                finished = true;
            }
        }
        for (Map.Entry<String, String> entry : macT.entrySet()) {
            try {
//                listaEndpoint.add(new EndpointDTO(entry.getKey(),"null",apiAp));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

        }
        return listaEndpoint;


    }


    public static String getSnmpValue(Snmp snmp, CommunityTarget target, OID oid) throws Exception {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(oid));
        pdu.setType(PDU.GET);

        ResponseEvent response = snmp.get(pdu, target);
        if (response != null && response.getResponse() != null) {
            return response.getResponse().get(0).getVariable().toString();
        }
        return "";
    }
    private static String formatMacAddress(OctetString mac) {
        byte[] macBytes = mac.getValue();
        StringBuilder macStr = new StringBuilder();
        for (byte b : macBytes) {
            if (macStr.length() > 0) {
                macStr.append(":");
            }
            macStr.append(String.format("%02X", b));
        }
        return macStr.toString();
    }

}
