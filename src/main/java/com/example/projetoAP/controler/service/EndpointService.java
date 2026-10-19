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
import jakarta.annotation.PreDestroy;

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
    private final ExecutorService pingExecutorService = Executors.newFixedThreadPool(10);
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
    
    // Controle de timeouts para ping threads
    private final Map<String, Long> ipLastActivityTime = new ConcurrentHashMap<>();
    private final long PING_THREAD_TIMEOUT_MS = 300000; // 5 minutos sem atividade
    public EndpointService( SalvaEndPoint salvaEndPoint){
        this.salvaEndPoint = salvaEndPoint;
    }
    
    @PreDestroy
    public void shutdown() {
        // Interrompe todos os ping threads
        for (Thread thread : pingThreads.values()) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }
        
        // Encerra os executor services
        executorService.shutdown();
        pingExecutorService.shutdown();
        
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!pingExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                pingExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            pingExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
        try (FileInputStream fis = new FileInputStream(new File(caminho));
             Workbook workbook = new XSSFWorkbook(fis)) {
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
        // Verifica se já existe thread para este IP
        if (ipToPingThread.containsKey(ip)) {
            ipLastActivityTime.put(ip, System.currentTimeMillis());
            return;
        }

        pingExecutorService.submit(() -> {
            try {
                ipLastActivityTime.put(ip, System.currentTimeMillis());
                
                while (!Thread.currentThread().isInterrupted()) {
                    // Verifica timeout - se não há sessões interessadas ou timeout foi excedido, para
                    Set<String> sessions = ipToSessionIds.getOrDefault(ip, Set.of());
                    if (sessions.isEmpty()) {
                        long lastActivity = ipLastActivityTime.getOrDefault(ip, System.currentTimeMillis());
                        long timeSinceLastActivity = System.currentTimeMillis() - lastActivity;
                        
                        if (timeSinceLastActivity > PING_THREAD_TIMEOUT_MS) {
                            break; // Sai do loop após timeout
                        }
                    }
                    
                    try {
                        Process p = Runtime.getRuntime().exec("ping -c 1 " + ip);
                        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String line;
                        while ((line = input.readLine()) != null) {
                            if (line.contains("ttl=") || line.contains("TTL=")) {
                                for (String sessionId : ipToSessionIds.getOrDefault(ip, Set.of())) {
                                    try {
                                        messagingTemplate.convertAndSend("/topic/cmd/" + sessionId, line);
                                    } catch (Exception e) {
                                        // Log e continua
                                    }
                                }
                            }
                        }
                        p.waitFor();
                    } catch (IOException | InterruptedException e) {
                        if (!Thread.currentThread().isInterrupted()) {
                            Thread.sleep(1000);
                        }
                    }
                    
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ipToPingThread.remove(ip);
                ipLastActivityTime.remove(ip);
            }
        });

        ipToPingThread.put(ip, Thread.currentThread()); // ConcurrentHashMap nao aceita null
    }

    private final Map<String, Thread> pingThreads = new ConcurrentHashMap<>();

    private static final java.util.regex.Pattern IP_VALIDO = java.util.regex.Pattern.compile("^[A-Za-z0-9.\\-]+$");
    private static final long CMD_MAX_MS = 10 * 60 * 1000; // nao deixa ping de terminal rodando para sempre

    public void CMDPing(cmdDTO dto) throws IOException {
        if (dto.ip() == null || !IP_VALIDO.matcher(dto.ip().trim()).matches()) {
            messagingTemplate.convertAndSend("/topic/cmd/" + dto.hostID(), "IP invalido");
            return;
        }
        // Se já existe uma thread para esse hostID, interrompe
        Thread existing = pingThreads.get(dto.hostID());
        if (existing != null && existing.isAlive()) {
            existing.interrupt();
            try {
                existing.join(5000); // Aguarda até 5s para a thread terminar
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            pingThreads.remove(dto.hostID());
        }
        
        Runnable pingTask = () -> {
            int consecutiveErrors = 0;
            final int MAX_CONSECUTIVE_ERRORS = 5; // Após 5 erros consecutivos, para
            
            long inicio = System.currentTimeMillis();
            try {
                while (!Thread.currentThread().isInterrupted()
                        && System.currentTimeMillis() - inicio < CMD_MAX_MS) {
                    try {
                        Process p = new ProcessBuilder("ping", "-c", "1", "-W", "2", dto.ip().trim())
                                .redirectErrorStream(true).start();
                        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String line;
                        boolean found = false;
                        
                        while ((line = input.readLine()) != null) {
                            if (line.contains("ttl=") || line.contains("TTL=")) {
                                try {
                                    messagingTemplate.convertAndSend("/topic/cmd/" + dto.hostID(), line);
                                    consecutiveErrors = 0;
                                    found = true;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        
                        if (!p.waitFor(5, TimeUnit.SECONDS)) p.destroyForcibly();
                        
                        // Se nenhuma resposta foi encontrada
                        if (!found) {
                            consecutiveErrors++;
                            if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                                break; // Para após múltiplas falhas
                            }
                        }
                        
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (IOException e) {
                        consecutiveErrors++;
                        if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                            break;
                        }
                        e.printStackTrace();
                        Thread.sleep(1000);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pingThreads.remove(dto.hostID());
            }
        };

        Thread pingThread = new Thread(pingTask);
        pingThread.setDaemon(true);
        pingThreads.put(dto.hostID(), pingThread);
        pingThread.start();
    }
    public void delete(Long id) {
        salvaEndPoint.delete(id);
    }
    @Transactional
    public void atualizarStatusParcial(Long id, String status, String ping, LocalDateTime ultima, LocalDateTime ultimaOff, boolean agoraIntermitente) {
        salvaEndPoint.atualizarStatus(id, status, ping,ultima,ultimaOff,agoraIntermitente);
    }
}
