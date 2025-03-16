package com.example.projetoAP.infra;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class Exeption {

    @ExceptionHandler(RuntimeException.class)
    public ModelAndView erro(IllegalAccessException e, Model model) {
        ModelAndView modelAndView = new ModelAndView("404");
        modelAndView.addObject("mensagem", e.getMessage()); // Passa uma mensagem para a página, se necessário
        return modelAndView;
    }
    // Exemplo de tratamento de exceção genérica
}
