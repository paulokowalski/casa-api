package com.kowalski.casaapi.api.v1.handler;

import com.kowalski.casaapi.api.v1.controller.ContaController;
import com.kowalski.casaapi.api.v1.controller.FaturaController;
import com.kowalski.casaapi.api.v1.controller.RendaController;
import com.kowalski.casaapi.business.exception.RecursoNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros de validação como 400 e "não encontrado" como 404, com a mensagem no corpo. */
@RestControllerAdvice(assignableTypes = {ContaController.class, RendaController.class, FaturaController.class})
public class FinancasExceptionHandler {

    public record Erro(String mensagem) {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Erro validacao(IllegalArgumentException e) {
        return new Erro(e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Erro corpoInvalido(HttpMessageNotReadableException e) {
        return new Erro("Dados inválidos na requisição");
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Erro naoEncontrado(RecursoNaoEncontradoException e) {
        return new Erro(e.getMessage());
    }
}
