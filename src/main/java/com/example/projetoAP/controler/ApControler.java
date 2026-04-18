package com.example.projetoAP.controler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Service.EndpointInterfaceDTO;
import com.example.projetoAP.controler.SetoresService.SetorService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.controler.service.CalcularPerdas;
import com.example.projetoAP.controler.service.EndpointService;
import com.example.projetoAP.controler.service.GroupService;
import com.example.projetoAP.domain.model.UpdateDTO;
import com.example.projetoAP.domain.vo.Filter;

import com.example.projetoAP.controler.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/endpoint")
@CrossOrigin(origins = "*")
public class ApControler {

    @Autowired
    CalcularPerdas calcularPerdas;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    GroupService groupService;
    @Autowired
    AuthService authService;
    @Autowired
    AuditService auditService;
    EndpointService endpointService;
    SetorService setorService;
    ApControler(EndpointService endpointService, SetorService setor){
        this.endpointService = endpointService;
        this.setorService = setor;
    }


    @GetMapping("/monitoramento")
    public String getVier(){
        return "index";
    }

    @PostMapping("/monitoramento")
    public String getVierPost(String id,Model model){
        model.addAttribute("v",id);
        return "index";
    }
    @GetMapping("/pagina")
    public String getVierPage(){
        return "pagina";
    }
    @PostMapping("/pagina")
    public String getVierListPost( String mac, Model model){
           model.addAttribute("nome",mac);
        return "pagina";
    }
    @GetMapping("/lista")
    public String getVierListPage(Model model, @RequestParam(name = "mac", required = false) String mac, @RequestParam(name = "ReceiveGrupoId",required = false) String ReceiveGrupoId, HttpSession sessionAttribute){
        if(ReceiveGrupoId!=null){
            sessionAttribute.setAttribute("search",ReceiveGrupoId);
        }
        List<EndpointDTO> l = new ArrayList<>();
       var re = sessionAttribute.getAttribute("search");
        if (mac != null && !mac.isEmpty()) {
            sessionAttribute.removeAttribute("search");
            // Filtra a lista baseado no MAC
            var lista = endpointService.getLista().stream()
                    .filter(e -> e.mac().contains(mac.trim())
                            ||e.ip().contains(mac.trim())
                            ||e.localizacao().contains(mac.trim())
                    ).toList();
          l.addAll(lista);
        }else if(re!=null){
            var lista = endpointService.getLista().stream()
                    .filter(e -> e.grupo().equals(re.toString())).toList();
            l.addAll(lista);
        }
        else {
            model.addAttribute("off",endpointService.getLista().stream().filter(e->e.status().equals("DOWN") ).toList());
            l.addAll( endpointService.getLista());
        }
        var lista = groupService.findAll();
        model.addAttribute("groups",lista);
        model.addAttribute("lista", l);
        // Passa a lista para o front-end
        return "lista";
    }
    @GetMapping("/antenas")
    public String getVierPagePost(Model model, String mac){
        model.addAttribute("nome",mac);
        return "antenas";
    }
    @GetMapping("/registro")
    public String setRegistro(Model model,Long id){
        if(id!=null){
           var hpst = endpointService.findOneById(id);
            model.addAttribute("form",hpst);
        }
        var lista = groupService.findAll();
        var listaSetor = setorService.getLista();
          model.addAttribute("groups",lista);
        model.addAttribute("setor",listaSetor);
        return "registro";
    }
    @PostMapping("/registro")
    @Transactional
    public String setRegistros(RedirectAttributes redirectAttributes,Model model, EndpointInterfaceDTO e) throws IOException, InterruptedException {
        System.out.println("chegou aqui");
       try {
          endpointService.setRegistro(e);
          redirectAttributes.addFlashAttribute("mensagem", "Registrado");
           return "redirect:/endpoint/registro";

       }catch (RuntimeException ex){
           redirectAttributes.addFlashAttribute("form",e);
           redirectAttributes.addFlashAttribute("erro", "Ocorreu um erro: " + ex.getMessage());
           return "redirect:/endpoint/registro";
       }

    }

    @GetMapping("erro")
    public String paginaNaoEncontrada(){
        return "404";
    }

    @PostMapping("/mult")
    public String mult(@RequestParam(value = "file",required = false) MultipartFile file,Model red, String ReceiveGrupoId,String nome) throws IOException, InterruptedException {
        String End = "/home/grupomateus/Área de trabalho/monitoramento/arquivos/";
        String caminho = End+file.getOriginalFilename();
        file.transferTo(new File(caminho));
        endpointService.salvaMultPartAsync(caminho,ReceiveGrupoId,nome);
        red.addAttribute("mensagem","Processando dados...");
        return "redirect:/registro";
    };
    @GetMapping("/web")
    public String websocket(){
        return "listaweb";
    }
    @GetMapping("/usuarios")
    public String usuarios(){
        return "usuarios";
    }
    @GetMapping("/historico")
    public String historico(){
        return "historico";
    }
    @GetMapping("/monitoramento/char")
    public String monitor(){
        return "charlista";
    }
    @GetMapping("/char")
    public String Char(@RequestParam(required = false, defaultValue = "0") String limit,
                       @RequestParam(required = false, defaultValue = "") String hostname,
                       @RequestParam(required = false, defaultValue = "0") String hostid,
                       @RequestParam(required = false, defaultValue = "1") String itemid, Model model) throws ExecutionException, InterruptedException {
        long limitValue = parseLongOrDefault(limit, 0L);
        long hostIdValue = parseLongOrDefault(hostid, 0L);
        long itemIdValue = parseLongOrDefault(itemid, 1L);

        if (hostIdValue <= 0) {
            model.addAttribute("data", Map.of("result", List.of()));
            return "char";
        }

        var lista = calcularPerdas.listaMetricas(new Filter(limitValue, itemIdValue, hostIdValue));
        model.addAttribute("data", lista.get());
        return "char";
    }

    private long parseLongOrDefault(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    @PutMapping("/registro")
    public String atualizaDados(UpdateDTO e,Model red){
        endpointService.atualizaEndpoint(e);
        return "redirect:/endpoint/lista";
    }
    @DeleteMapping("/delete")
    @Transactional
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deletar(@RequestParam Long id,
                                                               @RequestHeader(value = "Authorization", required = false) String token,
                                                               HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoDelete")) {
            return org.springframework.http.ResponseEntity.status(403).body(Map.of("message", "Sem permissao para deletar. Faca login novamente."));
        }
        String username = authService.getUsername(token);
        endpointService.delete(id);
        auditService.registrar(username, "DELETAR_ENDPOINT", "/endpoint/delete",
                "Deletou endpoint ID: " + id, getClientIp(request), request.getHeader("User-Agent"));
        return org.springframework.http.ResponseEntity.ok(Map.of("message", "Endpoint excluido com sucesso"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
///sudo docker run --name mysql -e MYSQL_DATABASE=informatica -e MYSQL_ROOT_PASSWORD=Tgfhvbnm1@ -p 3306:3306 -d mysql:latest