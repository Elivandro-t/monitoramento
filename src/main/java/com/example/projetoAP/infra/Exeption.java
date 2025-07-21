package com.example.projetoAP.infra;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class Exeption {

    @ExceptionHandler(NoResourceFoundException.class)
    public String erro(NoResourceFoundException e, Model model) {
//        ModelAndView modelAndView = new ModelAndView();
//    modelAndView.setViewName("404");// Passa uma mensagem para a página, se necessário
//        return modelAndView;
        return "redirect:/";
    }
    // Exemplo de tratamento de exceção genérica
}
