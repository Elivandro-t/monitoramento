package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.AuditService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.controler.service.EquipamentoCondenacaoService;
import com.example.projetoAP.controler.service.PdfService;
import com.example.projetoAP.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/manutencoes")
@CrossOrigin(origins = "*")
@Tag(name = "Manutencoes", description = "Registro de laudos tecnicos, upload de arquivos e imagens")
public class ManutencaoControler {

    private static final String UPLOAD_DIR = "uploads/manutencoes";

    private static final Set<String> STATUS_VALIDOS = Set.of(
            "AGUARDANDO_MANUTENCAO", "PENDENTE", "AGUARDANDO_PECAS", "EM_ANALISE",
            "CONDENADO", "FINALIZADO", "ENTREGUE");

    private static final Set<String> TIPOS_VALIDOS = Set.of("PREVENTIVA", "CORRETIVA");

    private final ManutencaoRepository manutencaoRepository;
    private final ManutencaoStatusLogRepository statusLogRepository;
    private final AgenteRepository agenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;
    private final AuditService auditService;
    private final PdfService pdfService;
    private final EquipamentoCondenacaoService condenacaoService;

    public ManutencaoControler(ManutencaoRepository manutencaoRepository,
                               ManutencaoStatusLogRepository statusLogRepository,
                               AgenteRepository agenteRepository,
                               UsuarioRepository usuarioRepository,
                               AuthService authService,
                               AuditService auditService,
                               PdfService pdfService,
                               EquipamentoCondenacaoService condenacaoService) {
        this.manutencaoRepository = manutencaoRepository;
        this.statusLogRepository = statusLogRepository;
        this.agenteRepository = agenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.pdfService = pdfService;
        this.condenacaoService = condenacaoService;
    }

    // ===== PDF: relatorio de manutencoes em aberto =====
    @Operation(summary = "Exporta manutencoes (exclui CONDENADAS) em PDF")
    @GetMapping("/relatorio.pdf")
    public ResponseEntity<byte[]> relatorioPdf(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) String setor,
                                               @RequestParam(required = false) String responsavel,
                                               @RequestParam(required = false) String download,
                                               @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.isAutorizado(token)) return ResponseEntity.status(401).build();
        String st = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        String sr = (setor == null || setor.isBlank()) ? null : setor.trim();
        String rs = (responsavel == null || responsavel.isBlank()) ? null : responsavel.trim();
        var lista = manutencaoRepository.filtrarAtivas(st, sr, rs).stream()
                .map(this::toMap).toList();
        byte[] pdf = pdfService.gerarRelatorioManutencoes(lista);
        String dispo = ("1".equals(download) ? "attachment" : "inline")
                + "; filename=\"manutencoes.pdf\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, dispo)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ===== LISTAR manutencoes de um agente =====
    @Operation(summary = "Lista manutencoes de um equipamento")
    @GetMapping("/agente/{agentId}")
    public ResponseEntity<?> listar(@PathVariable String agentId,
                                    @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var lista = manutencaoRepository.findByAgentIdOrderByDataManutencaoDesc(agentId)
                .stream().map(this::toMap).toList();
        return ResponseEntity.ok(lista);
    }

    // ===== CRIAR manutencao (apenas tecnico) =====
    // Recebe multipart: descricao, laudoFinal, tipoManutencao, status, arquivo (anexo), imagem
    @Operation(summary = "Registra nova manutencao (apenas tecnicos/admin). Aceita imagem e arquivo de laudo.")
    @PostMapping(value = "/agente/{agentId}", consumes = {"multipart/form-data"})
    @Transactional
    public ResponseEntity<?> criar(@PathVariable String agentId,
                                   @RequestParam(required = false) String descricao,
                                   @RequestParam(required = false) String laudoFinal,
                                   @RequestParam(required = false) String tipoManutencao,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) MultipartFile arquivo,
                                   @RequestParam(required = false) MultipartFile imagem,
                                   @RequestHeader(value = "Authorization", required = false) String token,
                                   HttpServletRequest request) throws IOException {
        if (!authService.isAutorizado(token)) {
            return ResponseEntity.status(401).body(Map.of("message", "Nao autenticado"));
        }
        String username = authService.getUsername(token);
        var userOpt = usuarioRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "Usuario nao encontrado"));
        }
        var user = userOpt.get();
        // Apenas tecnicos (cargo contem "TECNICO" ou perfil ADMIN/INFORMATICA) podem dar laudo
        boolean ehTecnico = (user.getCargo() != null && user.getCargo().toUpperCase().contains("TECNICO"))
                || "ADMIN".equals(user.getPerfil())
                || "INFORMATICA".equals(user.getPerfil());
        if (!ehTecnico) {
            return ResponseEntity.status(403).body(Map.of("message", "Apenas tecnicos podem registrar laudo"));
        }

        var agOpt = agenteRepository.findByAgentId(agentId);
        if (agOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Agente nao encontrado"));
        }
        var ag = agOpt.get();
        if(!ag.getStatus().equals("ENTREGUE")||!ag.getStatus().equals("CONDENADO")){
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Aviso: Equipamento ja possui manutencao ativa"));
        }

        Path baseDir = Paths.get(UPLOAD_DIR, agentId);
        Files.createDirectories(baseDir);

        String nomeUsuario = user.getNome() != null ? user.getNome() : username;
        String statusInicial = normalizeStatus(status, "AGUARDANDO_MANUTENCAO");
        if (!STATUS_VALIDOS.contains(statusInicial)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status invalido: " + statusInicial));
        }
        String tipo = tipoManutencao == null ? null : tipoManutencao.trim().toUpperCase();
        if (tipo != null && !tipo.isEmpty() && !TIPOS_VALIDOS.contains(tipo)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tipo invalido: " + tipo));
        }

        ManutencaoEntity m = new ManutencaoEntity();
        m.setAgentId(agentId);
        m.setTecnicoUsername(username);
        m.setTecnicoNome(nomeUsuario);
        m.setResponsavelUsername(username);
        m.setResponsavelNome(nomeUsuario);
        m.setSetor(ag.getSetor());
        m.setFilial(ag.getFilial());
        m.setPatrimonio(ag.getPatrimonio());
        m.setHostname(ag.getHostname());
        m.setDescricao(descricao);
        m.setLaudoFinal(laudoFinal);
        m.setStatus(statusInicial);
        m.setTipoManutencao(tipo);
        m.setDataManutencao(LocalDateTime.now());
        m.setDataEntrada(LocalDateTime.now());

        if (arquivo != null && !arquivo.isEmpty()) {
            String nome = sanitize(arquivo.getOriginalFilename());
            String armazen = "laudo_" + System.currentTimeMillis() + "_" + nome;
            Path dest = baseDir.resolve(armazen);
            Files.copy(arquivo.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            m.setAnexoArquivo(dest.toString().replace('\\', '/'));
            m.setAnexoOriginal(nome);
        }

        if (imagem != null && !imagem.isEmpty()) {
            String nome = sanitize(imagem.getOriginalFilename());
            String armazen = "img_" + System.currentTimeMillis() + "_" + nome;
            Path dest = baseDir.resolve(armazen);
            Files.copy(imagem.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            m.setImagem(dest.toString().replace('\\', '/'));
            // Tambem atualiza imagem principal do agente se ele nao tiver uma
            if (ag.getImagemUrl() == null || ag.getImagemUrl().isBlank()) {
                ag.setImagemUrl(m.getImagem());
            }
        }

        manutencaoRepository.save(m);

        // Log inicial de status
        statusLogRepository.save(new ManutencaoStatusLogEntity(
                m.getId(), agentId, null, statusInicial, username, nomeUsuario,
                "Abertura de manutencao"));

        // Atualiza contador e status do agente
        long total = manutencaoRepository.countByAgentId(agentId);
        ag.setNumManutencoes((int) total);
        ag.recalcularStatusUso();
        agenteRepository.save(ag);

        auditService.registrar(username, "CRIAR_MANUTENCAO", "/manutencoes/agente/" + agentId,
                "Registrou manutencao: id=" + m.getId() + " status=" + statusInicial,
                getClientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("message", "Manutencao registrada",
                "id", m.getId(), "status", statusInicial));
    }

    // ===== ALTERAR STATUS (exige reautenticacao usuario+senha) =====
    @Operation(summary = "Altera o status de uma manutencao (exige reautenticacao). " +
            "Para CONDENADO, use o endpoint /condenar com anexo obrigatorio.")
    @PostMapping(value = "/{id}/status", consumes = {"multipart/form-data"})
    @Transactional
    public ResponseEntity<?> alterarStatus(@PathVariable long id,
                                           @RequestParam String novoStatus,
                                           @RequestParam String usuario,
                                           @RequestParam String senha,
                                           @RequestParam(required = false) String observacao,
                                           @RequestParam(required = false) MultipartFile laudo,
                                           @RequestHeader(value = "Authorization", required = false) String token,
                                           HttpServletRequest request) throws IOException {
        if (!authService.isAutorizado(token)) {
            return ResponseEntity.status(401).body(Map.of("message", "Nao autenticado"));
        }
        String novo = normalizeStatus(novoStatus, null);
        if (novo == null || !STATUS_VALIDOS.contains(novo)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status invalido"));
        }
        if (!authService.revalidar(usuario, senha)) {
            return ResponseEntity.status(403).body(Map.of("message", "Usuario ou senha invalidos"));
        }

        var opt = manutencaoRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var m = opt.get();
        String anterior = m.getStatus();

        // ===== Regras de transicao de status =====
        // ENTREGUE so apos FINALIZADO; FINALIZADO e CONDENADO so apos EM_ANALISE.
        if ("ENTREGUE".equals(novo) && !"FINALIZADO".equals(anterior)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Transicao invalida: ENTREGUE so e permitido apos FINALIZADO. " +
                            "Status atual: " + anterior));
        }
        if ("FINALIZADO".equals(novo) && !"EM_ANALISE".equals(anterior)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Transicao invalida: FINALIZADO so e permitido apos EM_ANALISE. " +
                            "Status atual: " + anterior));
        }
        if ("CONDENADO".equals(novo) && !"EM_ANALISE".equals(anterior)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Transicao invalida: CONDENADO so e permitido apos EM_ANALISE. " +
                            "Status atual: " + anterior));
        }

        if ("CONDENADO".equals(novo)) {
            if (laudo == null || laudo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Laudo tecnico (arquivo) obrigatorio para CONDENADO"));
            }
            var userOpt = usuarioRepository.findByUsername(usuario);
            boolean ehTecnico = userOpt.isPresent() && (
                    (userOpt.get().getCargo() != null && userOpt.get().getCargo().toUpperCase().contains("TECNICO"))
                            || "ADMIN".equals(userOpt.get().getPerfil())
                            || "INFORMATICA".equals(userOpt.get().getPerfil()));
            if (!ehTecnico) {
                return ResponseEntity.status(403).body(Map.of(
                        "message", "Apenas tecnico pode condenar equipamento"));
            }
            Path baseDir = Paths.get(UPLOAD_DIR, m.getAgentId());
            Files.createDirectories(baseDir);
            String nome = sanitize(laudo.getOriginalFilename());
            Path dest = baseDir.resolve("laudo_final_" + System.currentTimeMillis() + "_" + nome);
            Files.copy(laudo.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            m.setAnexoArquivo(dest.toString().replace('\\', '/'));
            m.setAnexoOriginal(nome);
            if (observacao != null && !observacao.isBlank()) m.setLaudoFinal(observacao);
        }

        m.setStatus(novo);
        if ("ENTREGUE".equals(novo) || "FINALIZADO".equals(novo) || "CONDENADO".equals(novo)) {
            if (m.getDataEntrega() == null) m.setDataEntrega(LocalDateTime.now());
        }

      var stus =  manutencaoRepository.save(m);
//        var oldManutencao = manutencaoRepository.findById(m.getId()).orElseThrow();
//        String oldStatus = oldManutencao.getStatus();
//        // 1. Defina o agente como nulo inicialmente
//        AgenteEntity agente = null;
//
//// 2. Tente buscar pelo AgentId (se ele existir)
//        if (stus.getAgentId() != null) {
//            agente = agenteRepository.findByAgentId(stus.getAgentId()).orElse(null);
//        }
//
//// 3. SE NÃO achou, tente buscar pelo Patrimônio (se o patrimônio existir no objeto 'm')
//        if (agente == null && m.getPatrimonio() != null) {
//            agente = agenteRepository.findByPatrimonio(m.getPatrimonio()).orElse(null);
//        }
//
//// 4. Só incrementa se, depois de todas as tentativas, encontramos um agente
//        if (agente != null) {
//            agente.setNumManutencoes(agente.getNumManutencoes() + 1);
//            // Se não estiver usando @Transactional no método, descomente:
//            // agenteRepository.save(agente);
//        } else {
//            // Apenas logue o erro, não quebre a requisição do usuário
//            System.out.println("Aviso: Nao foi possivel identificar o agente para manutencao ID: " + id);
//        }

        var userOpt = usuarioRepository.findByUsername(usuario);
        String nomeUsuario = userOpt.map(u -> u.getNome() != null ? u.getNome() : u.getUsername()).orElse(usuario);

        statusLogRepository.save(new ManutencaoStatusLogEntity(
                m.getId(), m.getAgentId(), anterior, novo, usuario, nomeUsuario, observacao));

        auditService.registrar(usuario, "ALTERAR_STATUS_MANUTENCAO", "/manutencoes/" + id + "/status",
                "Status " + anterior + " -> " + novo, getClientIp(request), request.getHeader("User-Agent"));

        boolean snapshotCriado = false;
        if ("CONDENADO".equals(novo)) {
            var ag = agenteRepository.findByAgentId(m.getAgentId()).orElse(null);
            if (ag != null) {
                try {
                    condenacaoService.condenar(ag,
                            observacao != null ? observacao : "Condenado via manutencao " + id,
                            m.getLaudoFinal(), usuario);
                    snapshotCriado = true;
                } catch (IllegalStateException ignore) {
                    // ja condenado antes; mantem apenas o log de status
                }
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Status alterado");
        resp.put("id", id);
        resp.put("statusAnterior", anterior == null ? "" : anterior);
        resp.put("statusNovo", novo);
        resp.put("snapshotCondenacao", snapshotCriado);
        return ResponseEntity.ok(resp);
    }

    // ===== HISTORICO de logs de status =====
    @Operation(summary = "Retorna historico de mudancas de status de uma manutencao")
    @GetMapping("/{id}/historico-status")
    public ResponseEntity<?> historicoStatus(@PathVariable long id,
                                             @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.isAutorizado(token)) return ResponseEntity.status(401).build();
        var lista = statusLogRepository.findByManutencaoIdOrderByDataHoraDesc(id).stream().map(l -> {
            Map<String, Object> r = new HashMap<>();
            r.put("id", l.getId());
            r.put("statusAnterior", nz(l.getStatusAnterior()));
            r.put("statusNovo", nz(l.getStatusNovo()));
            r.put("usuario", nz(l.getUsuario()));
            r.put("usuarioNome", nz(l.getUsuarioNome()));
            r.put("observacao", nz(l.getObservacao()));
            r.put("dataHora", l.getDataHora() != null ?
                    l.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
            return r;
        }).toList();
        return ResponseEntity.ok(lista);
    }

    // ===== DETALHE de uma manutencao + snapshot do equipamento + historico =====
    @Operation(summary = "Detalhes completos de uma manutencao: info do equipamento, manutencao e historico do agente")
    @GetMapping("/{id}/detalhe")
    public ResponseEntity<?> detalheManutencao(@PathVariable long id,
                                               @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.temPermissao(token, "acessoUsuarios")) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var opt = manutencaoRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var m = opt.get();

        Map<String, Object> resp = new HashMap<>();
        resp.put("manutencao", toMap(m));

        var agOpt = agenteRepository.findByAgentId(m.getAgentId());
        if (agOpt.isPresent()) {
            var ag = agOpt.get();
            Map<String, Object> eq = new HashMap<>();
            eq.put("id", ag.getId());
            eq.put("agentId", nz(ag.getAgentId()));
            eq.put("hostname", nz(ag.getHostname()));
            eq.put("patrimonio", nz(ag.getPatrimonio()));
            eq.put("tipo", nz(ag.getTipo()));
            eq.put("marca", nz(ag.getMarca()));
            eq.put("modelo", nz(ag.getModelo()));
            eq.put("numeroSerie", nz(ag.getNumeroSerie()));
            eq.put("setor", nz(ag.getSetor()));
            eq.put("filial", nz(ag.getFilial()));
            eq.put("ip", nz(ag.getIp()));
            eq.put("anydesk", nz(ag.getAnydesk()));
            eq.put("dominio", nz(ag.getDominio()));
            eq.put("status", nz(ag.getStatus()));
            eq.put("statusUso", nz(ag.getStatusUso()));
            eq.put("numManutencoes", ag.getNumManutencoes());
            eq.put("temImagem", ag.getImagemUrl() != null && !ag.getImagemUrl().isBlank());
            resp.put("equipamento", eq);
        }

        var historico = manutencaoRepository.findByAgentIdOrderByDataManutencaoDesc(m.getAgentId())
                .stream().map(this::toMap).toList();
        resp.put("historico", historico);

        return ResponseEntity.ok(resp);
    }

    // ===== LISTAR manutencoes em aberto com filtros (rota /manutencoes/list) =====
    @Operation(summary = "Lista manutencoes (exclui CONDENADAS) com filtros opcionais")
    @GetMapping("/list")
    public ResponseEntity<?> listarAtivas(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String setor,
                                          @RequestParam(required = false) String responsavel,
                                          @RequestHeader(value = "Authorization", required = false) String token) {
        if (!authService.isAutorizado(token)) return ResponseEntity.status(401).build();
        String st = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        String sr = (setor == null || setor.isBlank()) ? null : setor.trim();
        String rs = (responsavel == null || responsavel.isBlank()) ? null : responsavel.trim();
        var lista = manutencaoRepository.filtrarAtivas(st, sr, rs).stream()
                .map(this::toMap).toList();
        return ResponseEntity.ok(lista);
    }

    private String normalizeStatus(String s, String def) {
        if (s == null || s.isBlank()) return def;
        return s.trim().toUpperCase();
    }

    // ===== EDITAR manutencao (apenas o tecnico que criou ou ADMIN) =====
    @Operation(summary = "Edita manutencao existente (somente autor ou admin)")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> editar(@PathVariable long id,
                                    @RequestBody Map<String, String> body,
                                    @RequestHeader(value = "Authorization", required = false) String token,
                                    HttpServletRequest request) {
        if (!authService.isAutorizado(token)) return ResponseEntity.status(401).build();
        String username = authService.getUsername(token);

        var opt = manutencaoRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var m = opt.get();
        if (!username.equals(m.getTecnicoUsername()) && !authService.isAdmin(token)) {
            return ResponseEntity.status(403).body(Map.of("message", "Somente o tecnico autor pode editar"));
        }
        if (body.containsKey("descricao")) m.setDescricao(body.get("descricao"));
        if (body.containsKey("laudoFinal")) m.setLaudoFinal(body.get("laudoFinal"));
        if (body.containsKey("tipoManutencao")) {
            String t = body.get("tipoManutencao");
            if (t != null) {
                t = t.trim().toUpperCase();
                if (!t.isEmpty() && !TIPOS_VALIDOS.contains(t)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Tipo invalido: " + t));
                }
                m.setTipoManutencao(t.isEmpty() ? null : t);
            }
        }
        if (body.containsKey("responsavelUsername")) {
            String r = body.get("responsavelUsername");
            m.setResponsavelUsername(r);
            if (r != null && !r.isBlank()) {
                usuarioRepository.findByUsername(r).ifPresent(u ->
                        m.setResponsavelNome(u.getNome() != null ? u.getNome() : u.getUsername()));
            }
        }
        manutencaoRepository.save(m);
        auditService.registrar(username, "EDITAR_MANUTENCAO", "/manutencoes/" + id,
                "Editou manutencao " + id, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("message", "Manutencao atualizada"));
    }

    // ===== DELETAR manutencao =====
    @Operation(summary = "Exclui manutencao (requer permissao acessoDelete ou admin)")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deletar(@PathVariable long id,
                                     @RequestHeader(value = "Authorization", required = false) String token,
                                     HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoDelete") && !authService.isAdmin(token)) {
            return ResponseEntity.status(403).body(Map.of("message", "Sem permissao"));
        }
        var opt = manutencaoRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        String agentId = opt.get().getAgentId();
        manutencaoRepository.deleteById(id);

        var agOpt = agenteRepository.findByAgentId(agentId);
        agOpt.ifPresent(ag -> {
            long total = manutencaoRepository.countByAgentId(agentId);
            ag.setNumManutencoes((int) total);
            ag.recalcularStatusUso();
            agenteRepository.save(ag);
        });

        auditService.registrar(authService.getUsername(token), "DELETAR_MANUTENCAO", "/manutencoes/" + id,
                "Deletou manutencao " + id, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("message", "Manutencao excluida"));
    }

    // ===== VISUALIZAR arquivo inline (sem download) =====
    @Operation(summary = "Visualiza o arquivo de laudo inline (sem forcar download)")
    @GetMapping("/{id}/arquivo")
    public ResponseEntity<Resource> visualizarArquivo(@PathVariable long id) throws IOException {
        var opt = manutencaoRepository.findById(id);
        if (opt.isEmpty() || opt.get().getAnexoArquivo() == null) return ResponseEntity.notFound().build();
        Path p = Paths.get(opt.get().getAnexoArquivo());
        if (!Files.exists(p)) return ResponseEntity.notFound().build();
        String contentType = URLConnection.guessContentTypeFromName(p.getFileName().toString());
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        Resource res = new FileSystemResource(p.toFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + opt.get().getAnexoOriginal() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(res);
    }

    // ===== DOWNLOAD do arquivo =====
    @Operation(summary = "Faz download do arquivo de laudo")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadArquivo(@PathVariable long id) throws IOException {
        var opt = manutencaoRepository.findById(id);
        if (opt.isEmpty() || opt.get().getAnexoArquivo() == null) return ResponseEntity.notFound().build();
        Path p = Paths.get(opt.get().getAnexoArquivo());
        if (!Files.exists(p)) return ResponseEntity.notFound().build();
        Resource res = new FileSystemResource(p.toFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + opt.get().getAnexoOriginal() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(res);
    }

    // ===== VISUALIZAR imagem inline =====
    @Operation(summary = "Visualiza a imagem vinculada a uma manutencao")
    @GetMapping("/{id}/imagem")
    public ResponseEntity<Resource> visualizarImagem(@PathVariable long id) throws IOException {
        var opt = manutencaoRepository.findById(id);
        if (opt.isEmpty() || opt.get().getImagem() == null) return ResponseEntity.notFound().build();
        Path p = Paths.get(opt.get().getImagem());
        if (!Files.exists(p)) return ResponseEntity.notFound().build();
        String contentType = URLConnection.guessContentTypeFromName(p.getFileName().toString());
        if (contentType == null) contentType = "image/jpeg";
        Resource res = new FileSystemResource(p.toFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(contentType))
                .body(res);
    }

    // ===== Upload imagem direta ao agente (sem criar manutencao) =====
    @Operation(summary = "Envia a imagem principal de um equipamento (foto do agente)")
    @PostMapping(value = "/agente/{agentId}/imagem", consumes = {"multipart/form-data"})
    @Transactional
    public ResponseEntity<?> uploadImagemAgente(@PathVariable String agentId,
                                                @RequestParam MultipartFile imagem,
                                                @RequestHeader(value = "Authorization", required = false) String token)
            throws IOException {
        if (!authService.isAutorizado(token)) return ResponseEntity.status(401).build();
        var agOpt = agenteRepository.findByAgentId(agentId);
        if (agOpt.isEmpty()) return ResponseEntity.notFound().build();
        Path baseDir = Paths.get(UPLOAD_DIR, agentId);
        Files.createDirectories(baseDir);
        String nome = sanitize(imagem.getOriginalFilename());
        Path dest = baseDir.resolve("equip_" + System.currentTimeMillis() + "_" + nome);
        Files.copy(imagem.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        var ag = agOpt.get();
        ag.setImagemUrl(dest.toString().replace('\\', '/'));
        agenteRepository.save(ag);
        return ResponseEntity.ok(Map.of("message", "Imagem salva", "imagemUrl", ag.getImagemUrl()));
    }

    // ===== Imagem do agente (inline) =====
    @Operation(summary = "Retorna a imagem principal do equipamento")
    @GetMapping("/agente/{agentId}/imagem")
    public ResponseEntity<Resource> imagemAgente(@PathVariable String agentId) throws IOException {
        var agOpt = agenteRepository.findByAgentId(agentId);
        if (agOpt.isEmpty() || agOpt.get().getImagemUrl() == null) return ResponseEntity.notFound().build();
        Path p = Paths.get(agOpt.get().getImagemUrl());
        if (!Files.exists(p)) return ResponseEntity.notFound().build();
        String contentType = URLConnection.guessContentTypeFromName(p.getFileName().toString());
        if (contentType == null) contentType = "image/jpeg";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(new FileSystemResource(p.toFile()));
    }

    private Map<String, Object> toMap(ManutencaoEntity m) {
        Map<String, Object> r = new HashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        r.put("id", m.getId());
        r.put("agentId", m.getAgentId());
        r.put("tecnicoUsername", nz(m.getTecnicoUsername()));
        r.put("tecnicoNome", nz(m.getTecnicoNome()));
        r.put("responsavelUsername", nz(m.getResponsavelUsername()));
        r.put("responsavelNome", nz(m.getResponsavelNome()));
        r.put("setor", nz(m.getSetor()));
        r.put("filial", nz(m.getFilial()));
        r.put("patrimonio", nz(m.getPatrimonio()));
        r.put("hostname", nz(m.getHostname()));
        r.put("descricao", nz(m.getDescricao()));
        r.put("laudoFinal", nz(m.getLaudoFinal()));
        r.put("status", nz(m.getStatus()));
        r.put("tipoManutencao", nz(m.getTipoManutencao()));
        r.put("anexoOriginal", nz(m.getAnexoOriginal()));
        r.put("temAnexo", m.getAnexoArquivo() != null);
        r.put("temImagem", m.getImagem() != null);
        r.put("data", m.getDataManutencao() != null ? m.getDataManutencao().format(fmt) : "");
        r.put("dataEntrada", m.getDataEntrada() != null ? m.getDataEntrada().format(fmt) : "");
        r.put("dataEntrega", m.getDataEntrega() != null ? m.getDataEntrega().format(fmt) : "");
        var dur = m.duracao();
        if (dur != null) {
            r.put("duracaoHoras", dur.toHours());
            r.put("duracaoDias", dur.toDays());
        } else {
            r.put("duracaoHoras", 0);
            r.put("duracaoDias", 0);
        }
        r.put("alerta", m.alertaTempo());
        return r;
    }

    private String sanitize(String name) {
        if (name == null) return "arquivo";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String nz(String s) { return s == null ? "" : s; }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isEmpty()) ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }
}
