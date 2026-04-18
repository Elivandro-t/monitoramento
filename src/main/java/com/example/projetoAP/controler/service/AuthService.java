package com.example.projetoAP.controler.service;

import com.example.projetoAP.repository.PerfilEntity;
import com.example.projetoAP.repository.PerfilRepository;
import com.example.projetoAP.repository.SessaoEntity;
import com.example.projetoAP.repository.SessaoRepository;
import com.example.projetoAP.repository.UsuarioEntity;
import com.example.projetoAP.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final SessaoRepository sessaoRepository;

    private record SessaoInfo(String username, String perfil, Map<String, Boolean> permissoes, LocalDateTime loginTime) {}

    private final Map<String, SessaoInfo> sessoes = new ConcurrentHashMap<>();

    public AuthService(UsuarioRepository usuarioRepository,
                       PerfilRepository perfilRepository,
                       SessaoRepository sessaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.sessaoRepository = sessaoRepository;
    }

    @PostConstruct
    @Transactional
    public void inicializarDados() {
        // Criar perfis padrao se nao existem
        if (perfilRepository.findByNome("ADMIN").isEmpty()) {
            perfilRepository.save(new PerfilEntity("ADMIN", "Acesso total ao sistema",
                    true, true, true, true, true));
        }
        if (perfilRepository.findByNome("INFORMATICA").isEmpty()) {
            perfilRepository.save(new PerfilEntity("INFORMATICA", "Acesso ao monitoramento e setores",
                    false, false, true, true, false));
        }

        // Criar ou atualizar usuario admin padrao
        var adminOpt = usuarioRepository.findByUsername("cupsadmin");
        if (adminOpt.isEmpty()) {
            String senhaHash = hashSenha("Netuno23");
            var admin = new UsuarioEntity("cupsadmin", senhaHash);
            admin.setNome("Administrador");
            admin.setPerfil("ADMIN");
            admin.setAutorizado(true);
            usuarioRepository.save(admin);
        } else {
            var admin = adminOpt.get();
            if (admin.getPerfil() == null || admin.getPerfil().isBlank()) {
                admin.setPerfil("ADMIN");
                admin.setAutorizado(true);
                if (admin.getNome() == null || admin.getNome().isBlank()) {
                    admin.setNome("Administrador");
                }
                usuarioRepository.save(admin);
            }
        }
    }

    @Transactional
    public Optional<Map<String, Object>> login(String username, String senha, String ipMaquina, String navegador) {
        Optional<UsuarioEntity> usuario = usuarioRepository.findByUsername(username);
        if (usuario.isEmpty()) return Optional.empty();

        var u = usuario.get();
        String senhaHash = hashSenha(senha);
        if (!senhaHash.equals(u.getSenha())) return Optional.empty();

        if (!u.isAutorizado()) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("error", "Usuario nao autorizado");
            return Optional.of(erro);
        }

        u.setUltimoLogin(LocalDateTime.now());
        usuarioRepository.save(u);

        Map<String, Boolean> permissoes = buscarPermissoes(u.getPerfil());
        String token = UUID.randomUUID().toString();
        sessoes.put(token, new SessaoInfo(u.getUsername(), u.getPerfil(), permissoes, LocalDateTime.now()));

        var sessaoEntity = new SessaoEntity(u.getUsername(), token, ipMaquina, navegador);
        sessaoRepository.save(sessaoEntity);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("token", token);
        resultado.put("username", u.getUsername());
        resultado.put("perfil", u.getPerfil());
        resultado.put("nome", u.getNome() != null ? u.getNome() : u.getUsername());
        resultado.put("permissoes", permissoes);
        return Optional.of(resultado);
    }

    private Optional<UsuarioEntity> carregarUsuario(String username) {
        return usuarioRepository.findByUsername(username);
    }

    private Map<String, Boolean> buscarPermissoes(String perfilNome) {
        Map<String, Boolean> perm = new HashMap<>();
        var perfilOpt = perfilRepository.findByNome(perfilNome);
        if (perfilOpt.isPresent()) {
            var p = perfilOpt.get();
            perm.put("acessoUsuarios", p.isAcessoUsuarios());
            perm.put("acessoHistorico", p.isAcessoHistorico());
            perm.put("acessoSetores", p.isAcessoSetores());
            perm.put("acessoRegistro", p.isAcessoRegistro());
            perm.put("acessoDelete", p.isAcessoDelete());
        } else {
            perm.put("acessoUsuarios", false);
            perm.put("acessoHistorico", false);
            perm.put("acessoSetores", false);
            perm.put("acessoRegistro", false);
            perm.put("acessoDelete", false);
        }
        return perm;
    }

    @Transactional
    public boolean isAutorizado(String token) {
        return carregarSessaoInfo(token) != null;
    }

    public boolean isAdmin(String token) {
        var sessao = carregarSessaoInfo(token);
        return sessao != null && "ADMIN".equals(sessao.perfil());
    }

    public boolean temPermissao(String token, String permissao) {
        var sessao = carregarSessaoInfo(token);
        if (sessao == null) return false;
        return Boolean.TRUE.equals(sessao.permissoes().get(permissao));
    }

    public String getUsername(String token) {
        var sessao = carregarSessaoInfo(token);
        return sessao != null ? sessao.username() : "anonimo";
    }

    public String getPerfil(String token) {
        var sessao = carregarSessaoInfo(token);
        return sessao != null ? sessao.perfil() : null;
    }

    public Map<String, Boolean> getPermissoes(String token) {
        var sessao = carregarSessaoInfo(token);
        return sessao != null ? sessao.permissoes() : Map.of();
    }

    public boolean revalidar(String username, String senha) {
        Optional<UsuarioEntity> usuario = usuarioRepository.findByUsername(username);
        if (usuario.isEmpty()) return false;
        String senhaHash = hashSenha(senha);
        return senhaHash.equals(usuario.get().getSenha()) && usuario.get().isAutorizado();
    }

    @Transactional
    public void logout(String token) {
        sessoes.remove(token);
        if (token == null || token.isBlank()) return;
        sessaoRepository.findByToken(token).ifPresent(sessao -> {
            sessao.encerrar();
            sessaoRepository.save(sessao);
        });
    }

    public List<Map<String, Object>> getSessoesAtivas() {
        return sessaoRepository.findByAtivaTrue().stream().map(sessao -> {
            Map<String, Object> dados = new HashMap<>();
            dados.put("id", sessao.getId());
            dados.put("usuario", sessao.getUsuario());
            dados.put("token", sessao.getToken());
            dados.put("horaEntrada", format(sessao.getHoraEntrada()));
            dados.put("horaExpiracao", format(sessao.getHoraExpiracao()));
            dados.put("ipMaquina", sessao.getIpMaquina());
            dados.put("navegador", sessao.getNavegador());
            return dados;
        }).toList();
    }

    public List<Map<String, Object>> getHistoricoSessoes() {
        return sessaoRepository.findTop100ByOrderByHoraEntradaDesc().stream().map(sessao -> {
            Map<String, Object> dados = new HashMap<>();
            dados.put("id", sessao.getId());
            dados.put("usuario", sessao.getUsuario());
            dados.put("token", sessao.getToken());
            dados.put("horaEntrada", format(sessao.getHoraEntrada()));
            dados.put("horaSaida", format(sessao.getHoraSaida()));
            dados.put("horaExpiracao", format(sessao.getHoraExpiracao()));
            dados.put("ativa", sessao.isAtiva());
            dados.put("ipMaquina", sessao.getIpMaquina());
            dados.put("navegador", sessao.getNavegador());
            return dados;
        }).toList();
    }

    private SessaoInfo carregarSessaoInfo(String token) {
        if (token == null || token.isBlank()) return null;
        var sessaoInfo = sessoes.get(token);
        if (sessaoInfo != null) {
            if (sessaoInfo.loginTime().isBefore(LocalDateTime.now().minusHours(5))) {
                sessoes.remove(token);
                sessaoRepository.findByToken(token).ifPresent(sessao -> {
                    if (sessao.isAtiva()) {
                        sessao.expirar();
                        sessaoRepository.save(sessao);
                    }
                });
                return null;
            }
            return sessaoInfo;
        }

        var sessaoOpt = sessaoRepository.findByToken(token);
        if (sessaoOpt.isEmpty()) return null;

        var sessaoEntity = sessaoOpt.get();
        if (!sessaoEntity.isAtiva() || sessaoEntity.getHoraExpiracao() == null || sessaoEntity.getHoraExpiracao().isBefore(LocalDateTime.now())) {
            if (sessaoEntity.isAtiva()) {
                sessaoEntity.expirar();
                sessaoRepository.save(sessaoEntity);
            }
            sessoes.remove(token);
            return null;
        }

        var usuarioOpt = carregarUsuario(sessaoEntity.getUsuario());
        if (usuarioOpt.isEmpty()) return null;
        var usuario = usuarioOpt.get();
        var permissoes = buscarPermissoes(usuario.getPerfil());
        var info = new SessaoInfo(usuario.getUsername(), usuario.getPerfil(), permissoes,
                sessaoEntity.getHoraEntrada() != null ? sessaoEntity.getHoraEntrada() : LocalDateTime.now());
        sessoes.put(token, info);
        return info;
    }

    private String format(LocalDateTime dataHora) {
        return dataHora != null ? dataHora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
    }

    public String hashSenha(String senha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(senha.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
