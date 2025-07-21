package com.example.projetoAP.controler;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Service.EndpointInterfaceDTO;
import com.example.projetoAP.controler.SetoresService.SetorService;
import com.example.projetoAP.controler.service.*;
import com.example.projetoAP.domain.model.UpdateDTO;
import com.example.projetoAP.domain.vo.Filter;
import com.example.projetoAP.domain.vo.MetricasDTO;
import jakarta.persistence.Table;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/endpoint")
@CrossOrigin(origins = "*")
public class ApControler {
    @Value("endpoint")
    private String endereco;
    @Autowired
    CalcularPerdas calcularPerdas;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    GroupService groupService;
    EndpointService endpointService;
    SetorService setorService;
    ApControler(EndpointService endpointService,SetorService setor){
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
                    .filter(e -> e.grupo().contains(re.toString())).toList();
            l.addAll(lista);
        }
        else {
            model.addAttribute("off",endpointService.getLista().stream().filter(e->e.status().contains("DOWN")).toList());
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
    @PostMapping
    public String setRegistros(RedirectAttributes redirectAttributes,Model model, EndpointInterfaceDTO e) throws IOException, InterruptedException {
       try {
          endpointService.setRegistro(e);
          redirectAttributes.addFlashAttribute("mensagem", "Registrado");
           return "redirect:/registro";

       }catch (RuntimeException ex){
           redirectAttributes.addFlashAttribute("form",e);
           redirectAttributes.addFlashAttribute("erro", "Ocorreu um erro: " + ex.getMessage());
           return "redirect:/registro";
       }

    }

    @GetMapping("erro")
    public String paginaNaoEncontrada(){
        return "404";
    }

    @PostMapping("/mult")
    public String mult(@RequestParam(value = "file",required = false) MultipartFile file,Model red, String ReceiveGrupoId,String nome) throws IOException, InterruptedException {
        String caminho = endereco+file.getOriginalFilename();
        file.transferTo(new File(caminho));
        endpointService.salvaMultPartAsync(caminho,ReceiveGrupoId,nome);
        red.addAttribute("mensagem","Processando dados...");
        return "redirect:/registro";
    };
    @GetMapping("/web")
    public String websocket(){
        return "listaweb";
    }
    @GetMapping("/monitoramento/char")
    public String monitor(){
        return "charlista";
    }
    @GetMapping("/char")
    public String Char(@RequestParam(required = false) String limit,
                       @RequestParam(required = false) String hostname,
                       @RequestParam(required = false) String hostid,
                       @RequestParam(required = false) String itemid, Model model) throws ExecutionException, InterruptedException {
        var lista = calcularPerdas.listaMetricas(new Filter(Long.parseLong(limit),Long.parseLong(itemid),Long.parseLong(hostid)));;
     Map<String,List<MetricasDTO>> li = lista.get();
        model.addAttribute("data",lista.get());
        return "char";
    }
    @PutMapping
    @Transactional
    public String atualizaDados(UpdateDTO e){
       endpointService.atualizaEndpoint(e);
       System.out.println("Meus dados chegaram aqui");
       return "registro";
    }
}
///sudo docker run --name mysql -e MYSQL_DATABASE=informatica -e MYSQL_ROOT_PASSWORD=Tgfhvbnm1@ -p 3306:3306 -d mysql:latest