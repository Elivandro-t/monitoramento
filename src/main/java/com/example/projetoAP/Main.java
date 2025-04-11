package com.example.projetoAP;

import com.example.projetoAP.controler.service.EndepointBuscadosService;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
//            String ipAddress = "10.88.4.35"; // IP do AP ou roteador
//            String community = "public"; // Comunidade SNMP (comum é "public")
//
//            // Criando uma conexão SNMP
//            try {
//
//                Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
//                snmp.listen();
//                Address targetAddress = new UdpAddress(ipAddress+"/161");
//                CommunityTarget target = new CommunityTarget();
//                target.setCommunity(new OctetString(community));
//                target.setAddress(targetAddress);
//                target.setVersion(SnmpConstants.version2c);
//                target.setRetries(3);
//                target.setTimeout(5000);
//
//                // OID do dispositivo conectado (modifique conforme necessário)
//                OID oid = new OID("1.3.6.1.2.1.17.4.3.1.1");
//// Exemplo de OID.1.3.6.1.2.1.1.1.0
//                PDU pdu = new PDU();
//                pdu.add(new VariableBinding(oid));
//                pdu.setType(PDU.GETNEXT);
//                List<String> macAddresses = new ArrayList<>();
//
//                boolean finished = false;
//
//                // Enviar requisição SNMP
//                while (!finished) {
//                    ResponseEvent response = snmp.getNext(pdu, target);
//                    if (response != null && response.getResponse() != null) {
//                        PDU responsePDU = response.getResponse();
//                        VariableBinding vb = responsePDU.get(0);
//
//                        if (vb.getOid().startsWith(oid)) {
//                            // Convertendo o MAC Address da resposta
//                            String mac = vb.getVariable().toString();
//                            macAddresses.add(mac);
//                            pdu.clear();
//                            pdu.add(new VariableBinding(vb.getOid())); // Próximo OID
//                        } else {
//                            finished = true;
//                        }
//                    } else {
//                        finished = true;
//                    }
//                }
//
//                snmp.close();
//                if (macAddresses.isEmpty()) {
//                    System.out.println("Nenhum dispositivo conectado encontrado!");
//                } else {
//                    System.out.println("Dispositivos conectados ao AP:");
//                    for (String mac : macAddresses) {
//                        System.out.println("- " + mac);
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

    }

}
