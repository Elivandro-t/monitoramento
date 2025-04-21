package com.example.projetoAP.Service;

import com.example.projetoAP.EndpointDTO;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.example.projetoAP.Service.MacConectados.getSnmpValue;
//@Service
public class MainService {
   public void main()throws Exception{
        List<EndpointDTO> allEndpoints = new ArrayList<>();
           MacConectados macConectados = new MacConectados();
           // Lista para armazenar todos os endpoints

           // IP do AP ou roteador
           Snmp snmp = null;
           try {
               Set<String> ipAddressList = new HashSet<>();
               ipAddressList.add("10.88.4.84");
               snmp = new Snmp(new DefaultUdpTransportMapping());
               snmp.listen();

               for (String ipAddress : ipAddressList) {
                   Address targetAddress = new UdpAddress(ipAddress + "/161");
                   CommunityTarget target = new CommunityTarget();
                   target.setCommunity(new OctetString("public"));
                   target.setAddress(targetAddress);
                   target.setVersion(SnmpConstants.version2c);
                   target.setRetries(3);
                   target.setTimeout(5000);
                    macConectados.getConectados(snmp, target);

               }

           }catch (Exception e){
               e.printStackTrace();
           }finally {
               if(snmp!=null){
                   snmp.close();

               }
           }

   }
}

