package com.example.projetoAP.infra;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class Exeption {

    @ExceptionHandler(NoResourceFoundException.class)
    public String erro(NoResourceFoundException e, Model model) {
        model.addAttribute("mensagem", "Recurso não encontrado.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String erroGenerico(Exception e, Model model) {
        model.addAttribute("mensagem", "Ocorreu um erro interno. Tente novamente mais tarde.");
        return "error";
    }
}
