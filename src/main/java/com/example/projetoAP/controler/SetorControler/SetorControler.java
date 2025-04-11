package com.example.projetoAP.controler.SetorControler;

import com.example.projetoAP.controler.SetoresService.SetorService;
import com.example.projetoAP.controler.SetoresService.SetorServices;
import com.example.projetoAP.domain.model.Setor.SetorDTO;
import com.example.projetoAP.domain.model.Setor.SetorRegistroDTO;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@CrossOrigin(origins = "*")
@RequestMapping
public class SetorControler {
    SetorService setorService;
    public SetorControler(SetorService service){
        this.setorService = service;
    }

    @GetMapping("/setores")
    public String getRegistro() {
        return "setores";
    }
    @PostMapping("/setores")
    @Transactional
    public String setRegistro(SetorRegistroDTO s, RedirectAttributes redirectAttributes) {
        setorService.setRegistro(s);
        redirectAttributes.addAttribute("msg","registrado");
        return "redirect:/setores";
    }
    @GetMapping("/setores/lista")
    public String getLista(Model model) {
       var lista = setorService.getLista();
        return "/registro";
    }
}
