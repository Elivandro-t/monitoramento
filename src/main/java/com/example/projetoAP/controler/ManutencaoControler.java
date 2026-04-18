package com.example.projetoAP.controler;

import com.example.projetoAP.controler.service.AuditService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.repository.AgenteRepository;
import com.example.projetoAP.repository.ManutencaoEntity;
import com.example.projetoAP.repository.ManutencaoRepository;
import com.example.projetoAP.repository.UsuarioRepository;
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

    private final ManutencaoRepository manutencaoRepository;
    private final AgenteRepository agenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;
    private final AuditService auditService;

    public ManutencaoControler(ManutencaoRepository manutencaoRepository,
                               AgenteRepository agenteRepository,
                               UsuarioRepository usuarioRepository,
                               AuthService authService,
                               AuditService auditService) {
        this.manutencaoRepository = manutencaoRepository;
        this.agenteRepository = agenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
        this.auditService = auditService;
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
    // Recebe multipart: descricao, laudoFinal, arquivo (anexo), imagem
    @Operation(summary = "Registra nova manutencao (apenas tecnicos/admin). Aceita imagem e arquivo de laudo.")
    @PostMapping(value = "/agente/{agentId}", consumes = {"multipart/form-data"})
    @Transactional
    public ResponseEntity<?> criar(@PathVariable String agentId,
                                   @RequestParam(required = false) String descricao,
                                   @RequestParam(required = false) String laudoFinal,
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

        Path baseDir = Paths.get(UPLOAD_DIR, agentId);
        Files.createDirectories(baseDir);

        ManutencaoEntity m = new ManutencaoEntity();
        m.setAgentId(agentId);
        m.setTecnicoUsername(username);
        m.setTecnicoNome(user.getNome() != null ? user.getNome() : username);
        m.setDescricao(descricao);
        m.setLaudoFinal(laudoFinal);
        m.setDataManutencao(LocalDateTime.now());

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

        // Atualiza contador e status do agente
        long total = manutencaoRepository.countByAgentId(agentId);
        ag.setNumManutencoes((int) total);
        ag.recalcularStatusUso();
        agenteRepository.save(ag);

        auditService.registrar(username, "CRIAR_MANUTENCAO", "/manutencoes/agente/" + agentId,
                "Registrou manutencao: id=" + m.getId(), getClientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("message", "Manutencao registrada", "id", m.getId()));
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
        r.put("id", m.getId());
        r.put("agentId", m.getAgentId());
        r.put("tecnicoUsername", nz(m.getTecnicoUsername()));
        r.put("tecnicoNome", nz(m.getTecnicoNome()));
        r.put("descricao", nz(m.getDescricao()));
        r.put("laudoFinal", nz(m.getLaudoFinal()));
        r.put("anexoOriginal", nz(m.getAnexoOriginal()));
        r.put("temAnexo", m.getAnexoArquivo() != null);
        r.put("temImagem", m.getImagem() != null);
        r.put("data", m.getDataManutencao() != null ?
                m.getDataManutencao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
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
