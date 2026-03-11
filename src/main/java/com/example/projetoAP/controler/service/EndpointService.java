package com.example.projetoAP.controler.service;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Service.EndpointInterfaceDTO;
import com.example.projetoAP.domain.model.Endpoint;
import com.example.projetoAP.domain.model.Setores;
import com.example.projetoAP.domain.model.UpdateDTO;
import com.example.projetoAP.repository.EntitySetor.RepositorySetor;
import com.example.projetoAP.repository.GroupRepository;
import com.example.projetoAP.repository.SalvaEndPoint;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class EndpointService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final BlockingQueue<Row> filaDeProcessamento = new LinkedBlockingQueue<>();
    private
    @Autowired
    GroupRepository groupService;
    @Autowired
    private RepositorySetor repositorySetor;
    private int totalLinhas = 0; // Total de linhas a processar
    private int linhasProcessadas = 0;
    SalvaEndPoint salvaEndPoint;
    private String id ;
    private String setores;
    public EndpointService( SalvaEndPoint salvaEndPoint){
        this.salvaEndPoint = salvaEndPoint;
    }
    public EndpointDTO setRegistro(EndpointInterfaceDTO e) throws IOException, InterruptedException {
        var l = groupService.getReferenceById(Long.parseLong(e.ReceiveGrupoId()));
        var setor = repositorySetor.findById(e.setorID()).get();
        var resposta = new Endpoint(e,l.getName(),new Setores(setor));
        return salvaEndPoint.GetSalva(resposta);
    }
    public List<EndpointDTO> getLista(){
        var lista = salvaEndPoint.GetLista();
        return lista;
    }
    public List<EndpointDTO> getListaDown(){
        var lista = salvaEndPoint.GetLista();
        return lista;
    }
    public String salvaMultPartAsync(String caminho, String p,String nome) throws IOException, InterruptedException {
       this.setores = nome;
        try (FileInputStream fis = new FileInputStream(new File(caminho))) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getRowNum() == 0) {  // Ignorar o cabeçalho
                    continue;
                }
                totalLinhas++;
              filaDeProcessamento.offer(row);
            }
        }
        this.id = p;
        executorService.submit(this::processarDados);
        enviarMensagemParaWebSocket();
        return "Processando...";
    }
    @Transactional
    protected void processarDados() {
        while (!filaDeProcessamento.isEmpty()) {
            try {
                Row row = filaDeProcessamento.poll(10, TimeUnit.SECONDS); // Aguarda até 10s por uma nova linha
                if (row != null) {
                    processarLinha(row);
                    linhasProcessadas++;
                    enviarMensagemParaWebSocket();
                }else{
                    linhasProcessadas = 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        enviarMensagemParaWebSocket();
    }
    private void processarLinha(Row texto) throws IOException, InterruptedException {
        var setor = repositorySetor.findOneNameComplexo(setores);

    try {
        String localization = String.valueOf(texto.getCell(0));
        String mac = String.valueOf(texto.getCell(1));
        String hostname = String.valueOf(texto.getCell(2));
        String ipv4 = String.valueOf(texto.getCell(4));
        String modelo = String.valueOf(texto.getCell(5));
        String sn = String.valueOf(texto.getCell(6));

        String json = """
                localization: %s
                Mac: %s
                hostname: %s
                String: %s
                String: %s
                sn: %s
                """.formatted(localization,mac,hostname,ipv4,modelo,sn);
        System.out.println(json);
        var setorw = repositorySetor.findById(setor.getId()).get();
        EndpointInterfaceDTO ed = new EndpointInterfaceDTO(hostname.replace("GM-",""), modelo.toUpperCase(), sn, localization.replace("SLZ-",""),mac, ipv4, "", "", "20/10/2023", "",this.id, setor.getFilial(),setor.getNameComplexo(),setor.getId());
        var l = groupService.findById(Long.parseLong(ed.ReceiveGrupoId())).orElseThrow(()->new RuntimeException("nenhum resultado"));
        var resposta = new Endpoint(ed, l.getName(),new Setores(setorw));
        salvaEndPoint.GetSalvas(resposta);
    }catch (Exception e){
        e.printStackTrace();
    }
    }

    private void enviarMensagemParaWebSocket() {
        String mensagem = String.format("Processando... %d/%d", linhasProcessadas, totalLinhas);
        messagingTemplate.convertAndSend("/topic/processamento", mensagem);  // Envia para o tópico que o cliente está ouvindo
    }

    public void atualizaEndpoint(UpdateDTO e) {
      try {
          salvaEndPoint.findOndeById(e);
      }catch (IOException | InterruptedException v){
          v.getStackTrace();
      }
    }
    public EndpointDTO findOneById(Long id) {
       return salvaEndPoint.findOneById(id);
    }
    Map<String, Set<String>> ipToSessionIds = new ConcurrentHashMap<>(); // IP → sessões interessadas
    Map<String, Thread> ipToPingThread = new ConcurrentHashMap<>();      // IP → thread de ping
    public void userSubscribedToIp(String ip, String sessionId) {
        ipToSessionIds.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        if (!ipToPingThread.containsKey(ip)) {
            startPing(ip);
        }
    }
    public void startPing(String ip) {
        Thread pingThread = new Thread(() -> {
            try {
                while (!ipToSessionIds.getOrDefault(ip, Set.of()).isEmpty()) {
                    Process p = Runtime.getRuntime().exec("ping -c 1 " + ip);
                    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while ((line = input.readLine()) != null) {
                        if (line.contains("ttl=") || line.contains("TTL=")) {
                            for (String sessionId : ipToSessionIds.get(ip)) {
                                messagingTemplate.convertAndSend("/topic/cmd/" + sessionId, line);
                            }
                        }
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ipToPingThread.remove(ip); // limpa depois que parar
            }
        });

        ipToPingThread.put(ip, pingThread);
        pingThread.start();
    }

    private final Map<String, Thread> pingThreads = new ConcurrentHashMap<>();

    public void CMDPing(cmdDTO dto) throws IOException {
        // Se já existe uma thread para esse hostID, interrompe
        Thread existing = pingThreads.get(dto.hostID());
        if (existing != null && existing.isAlive()) {
            existing.interrupt();
            pingThreads.remove(dto.hostID());
        }
        Thread pingThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Process p = Runtime.getRuntime().exec("ping -c 1 " + dto.ip());
                    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while ((line = input.readLine()) != null) {
                        if (line.contains("ttl=") || line.contains("TTL=")) {
                            messagingTemplate.convertAndSend("/topic/cmd/" + dto.hostID(), line);
                        }
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        pingThread.start();
        pingThreads.put(dto.hostID(), pingThread);
    }
    public void delete(Long id) {
        salvaEndPoint.delete(id);
    }
    @Transactional
    public void atualizarStatusParcial(Long id, String status, String ping,LocalDateTime ultima) {
        salvaEndPoint.atualizarStatus(id, status, ping,ultima);
    }
}
