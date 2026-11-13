package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.response.DespesaAnualResponse;
import com.kowalski.casaapi.api.v1.response.DespesaResponse;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.business.exception.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.service.DespesaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/despesas", produces = MediaType.APPLICATION_JSON_VALUE)
public class DespesaController {

    private final DespesaService despesaService;
    private final UsuarioLogado usuarioLogado;

    @GetMapping("/{ano}/{mes}")
    public DespesaResponse doMes(@PathVariable String ano, @PathVariable String mes, @RequestParam String pessoa) {
        DespesaResponse r = despesaService.buscarPorAnoMesNome(ano, mes, usuarioLogado.nomePessoa(pessoa));
        if (r == null) throw new RecursoNaoEncontradoException("Nenhuma despesa no mês");
        return r;
    }

    @GetMapping("/{ano}")
    public DespesaAnualResponse doAno(@PathVariable String ano, @RequestParam String pessoa) {
        DespesaAnualResponse r = despesaService.buscarDespesasAnuais(ano, usuarioLogado.nomePessoa(pessoa));
        if (r == null || r.despesasPorMes().isEmpty()) throw new RecursoNaoEncontradoException("Nenhuma despesa no ano");
        return r;
    }
}
