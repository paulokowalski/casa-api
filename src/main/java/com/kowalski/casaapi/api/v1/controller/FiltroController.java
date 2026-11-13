package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.response.FiltroResponse;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.business.exception.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.service.FiltroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/filtros", produces = MediaType.APPLICATION_JSON_VALUE)
public class FiltroController {

    private final FiltroService filtroService;
    private final UsuarioLogado usuarioLogado;

    private static List<FiltroResponse> lista(List<FiltroResponse> itens) {
        if (itens == null) throw new RecursoNaoEncontradoException("Nenhuma opção encontrada");
        return itens;
    }

    @GetMapping("/anos")
    public List<FiltroResponse> anos() {
        return lista(filtroService.buscarAnos());
    }

    @GetMapping("/meses")
    public List<FiltroResponse> meses(@RequestParam String ano) {
        return lista(filtroService.buscarMeses(ano));
    }

    @GetMapping("/pessoas")
    public List<FiltroResponse> pessoas(@RequestParam String ano, @RequestParam String mes) {
        List<FiltroResponse> pessoas = lista(filtroService.buscarPessoas(ano, mes));
        if (usuarioLogado.isAdmin()) return pessoas;
        return pessoas.stream().filter(p -> usuarioLogado.podeAcessarNome(p.codigo())).toList();
    }

    @GetMapping("/cartoes")
    public List<FiltroResponse> cartoes(@RequestParam String ano, @RequestParam String mes, @RequestParam String pessoa) {
        return lista(filtroService.buscarCartoes(ano, mes, usuarioLogado.nomePessoa(pessoa)));
    }

    @GetMapping("/categorias")
    public List<FiltroResponse> categorias() {
        return lista(filtroService.buscarCategorias());
    }

    @GetMapping("/tipos-lancamento")
    public List<FiltroResponse> tiposLancamento() {
        return lista(filtroService.buscarTiposDeLancamentos());
    }
}
