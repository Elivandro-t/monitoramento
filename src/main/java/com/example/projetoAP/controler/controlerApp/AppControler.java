package com.example.projetoAP.controler.controlerApp;

import org.apache.http.protocol.HTTP;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping
public class AppControler {

    private final ConcurrentHashMap<String,String> tokenApp = new ConcurrentHashMap<>();
   private final String versionApp = "1.0";
    @GetMapping("/download-app/{token}")
    public ResponseEntity<?> DownloadsApp(@PathVariable String token){
        var fileName = tokenApp.get(token);
        if(fileName==null){
            return ResponseEntity.status(403).body(Map.of("erro", "Erro gerar o app"));

        }
        try {
            var equipamento = getClass().getClassLoader().getResourceAsStream("static/monitoramento.apk");

            if(equipamento==null){
                return ResponseEntity.status(403).body(Map.of("erro", "Erro gerar o app"));

            }
            tokenApp.remove(token);
            InputStreamResource resource = new InputStreamResource(equipamento);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename="+fileName)
                    .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                    .body(resource);
        }catch (Exception e){}
        return ResponseEntity.status(403).body(Map.of("erro", "Erro gerar o app"));

    }
    @GetMapping("/app-downLoads")
    public ResponseEntity<AppDTO> DownloadConfig(){
      String token = UUID.randomUUID().toString();

      tokenApp.put(token,"monitoramento.apk");
      String urlApp = "http://192.168.88.70:8080/download-app/"+token;
      var resposta = new AppDTO("App-monitoramento ",versionApp,urlApp);
      return ResponseEntity.ok(resposta);
    }
}
