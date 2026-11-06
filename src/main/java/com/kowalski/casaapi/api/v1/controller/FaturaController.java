package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.input.FaturaInput;
import com.kowalski.casaapi.api.v1.response.FaturaResponse;
import com.kowalski.casaapi.business.service.FaturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static com.kowalski.casaapi.util.Competencias.de;

/** Fatura do cartão paga ou não no mês (usada na lista de contas da Gestão Financeira). */
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/fatura", produces = MediaType.APPLICATION_JSON_VALUE)
public class FaturaController {

    private final FaturaService faturaService;

    @GetMapping("/{ano}/{mes}")
    public FaturaResponse obter(@PathVariable int ano, @PathVariable int mes, @RequestParam Long pessoaId) {
        return faturaService.obter(pessoaId, de(ano, mes));
    }

    @PutMapping("/{ano}/{mes}")
    public FaturaResponse salvar(@PathVariable int ano, @PathVariable int mes, @RequestParam Long pessoaId,
                                 @RequestBody FaturaInput input) {
        return faturaService.salvar(pessoaId, de(ano, mes), input);
    }
}
