package com.example.projetoAP.controler.service;
import com.example.projetoAP.EndpointDTO;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
public class SnmpService {
    @Async
    public CompletableFuture<Boolean> snmp(EndpointDTO ap, String macAds) throws IOException {
          int numero = 0;
        // Criando uma conexão SNMP
        try {

            Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
            snmp.listen();
            Address targetAddress = new UdpAddress(ap.ip() + "/161");
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString("public"));
            target.setAddress(targetAddress);
            target.setVersion(SnmpConstants.version2c);
            target.setRetries(3);
            target.setTimeout(5000);

            // OID do dispositivo conectado (modifique conforme necessário)
            OID oid = new OID("1.3.6.1.2.1.17.4.3.1.1");
// Exemplo de OID.1.3.6.1.2.1.1.1.0
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(oid));
            pdu.setType(PDU.GETNEXT);

            boolean finished = false;

            // Enviar requisição SNMP
            while (!finished) {
                ResponseEvent response = snmp.getNext(pdu, target);
                if (response != null && response.getResponse() != null) {
                    PDU responsePDU = response.getResponse();
                    VariableBinding vb = responsePDU.get(0);
                    numero++;

                    if (vb.getOid().startsWith(oid)) {
                        // Convertendo o MAC Address da resposta
                        String mac = vb.getVariable().toString();
                        System.out.println("MAC Encontrado " + mac);

                        if (mac.equals(macAds)) {
                            System.out.println("MAC Encontrado " + mac);
                            return CompletableFuture.completedFuture(true);  // Dispositivo encontrado

                        }
                        pdu.add(new VariableBinding(vb.getOid())); // Próximo OID
                    } else {
                        finished = true;
                    }
                } else {
                    finished = true;
                }
            }
            snmp.close();

//                if (macAds.isEmpty()) {
//                    System.out.println("Nenhum dispositivo conectado encontrado!");
//                } else {
//                    System.out.println("Dispositivos conectados ao AP:");
//                    for (String mac : macAddresses) {
//                        System.out.println("- " + mac);
//                    }
//                }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        TransportMapping transport = new DefaultUdpTransportMapping();
//        Snmp snmp = new Snmp(transport);
//        snmp.listen();
//        Address targetAddress = new UdpAddress(ap.ip() + "/161");
//        CommunityTarget target = new CommunityTarget();
//        target.setCommunity(new OctetString("public"));
//        target.setAddress(targetAddress);
//        target.setVersion(SnmpConstants.version2c);
//        target.setRetries(3);
//        target.setTimeout(5000);
//        // Realiza a consulta SNMP
//        OID macTableOID = new OID("1.3.6.1.2.1.17.4.3.1.1"); // MACs conectados
//        PDU pdu = new PDU();
//        pdu.add(new VariableBinding(macTableOID));
//        pdu.setType(PDU.GETNEXT);
//        ResponseEvent response = snmp.send(pdu, target);
//        if (response != null) {
//            PDU responsePDU = response.getResponse();
//            if (responsePDU != null) {
//                for (VariableBinding vb : responsePDU.getVariableBindings()) {
//                    String mac = String.valueOf(vb.getVariable());
//                    System.out.println("IP vindo da rede "+ vb.getVariable().toString());
//                    if (mac.equals(macAds)) {
//                        return CompletableFuture.completedFuture(true);  // Dispositivo encontrado
//                    }
//                }
//            }
//        }
System.out.println("Processando "+numero);
        return CompletableFuture.completedFuture(false);  // Dispositivo não encontrado no AP


    }
}
