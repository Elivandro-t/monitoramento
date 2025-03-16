package com.example.projetoAP.controler;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Service.EndpointInterfaceDTO;
import com.example.projetoAP.controler.service.EndpointService;
import com.example.projetoAP.controler.service.GroupService;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Controller
@RequestMapping
public class ApControler {
    @Autowired
    GroupService groupService;
    EndpointService endpointService;
    ApControler(EndpointService endpointService){
        this.endpointService = endpointService;
    }


    @GetMapping("var")
    public String getVier(){
        return "index";
    }

    @PostMapping("/var")
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
    public String getVierListPage(Model model, String mac){
        List<EndpointDTO> l = new ArrayList<>();
        if (mac != null && !mac.isEmpty()) {
            // Filtra a lista baseado no MAC
            var lista = endpointService.getLista().stream()
                    .filter(e -> e.mac().contains(mac.trim())
                            ||e.ip().contains(mac.trim())
                            ||e.sn().contains(mac.trim())
                    ).toList();
          l.addAll(lista);
        } else {
            l.addAll( endpointService.getLista());
            // Caso o MAC seja null, adiciona a lista completa
        }
        model.addAttribute("off",endpointService.getLista().stream().filter(e->e.status().contains("DOWN")).toList());
        model.addAttribute("lista", l); // Passa a lista para o front-end
        return "lista";
    }
    @GetMapping("/antenas")
    public String getVierPagePost(Model model, String mac){
        model.addAttribute("nome",mac);
        return "antenas";
    }
    @GetMapping("/registro")
    public String setRegistro(Model model){
          var lista = groupService.findAll();
          model.addAttribute("groups",lista);


        return "registro";
    }
    @PostMapping("/registro")
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
    public String mult(@RequestParam("file") MultipartFile file,Model red) throws IOException, InterruptedException {
        System.out.println("Meu arquivo "+file.getInputStream().toString());
        System.out.println("Nome do meu arquivo "+file.getOriginalFilename());
        String caminho = "/home/elivandro/Downloads/projetoAP/"+file.getOriginalFilename();
        file.transferTo(new File(caminho));
        endpointService.salvaMultPartAsync(caminho);
        red.addAttribute("mensagem","Processando dados...");

        return "registro";
    }
}


///sudo docker run --name mysql -e MYSQL_DATABASE=informatica -e MYSQL_ROOT_PASSWORD=Tgfhvbnm1@ -p 3306:3306 -d mysql:latest