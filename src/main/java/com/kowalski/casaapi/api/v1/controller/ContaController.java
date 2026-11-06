package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.input.ContaInput;
import com.kowalski.casaapi.api.v1.input.LancamentoInput;
import com.kowalski.casaapi.api.v1.response.ContaResponse;
import com.kowalski.casaapi.api.v1.response.LancamentoResponse;
import com.kowalski.casaapi.business.service.ContaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.kowalski.casaapi.util.Competencias.parse;
import static com.kowalski.casaapi.util.Competencias.parseOuNulo;

/** Contas fora do cartão de crédito: financiamentos, prestações, plano de saúde, contas de consumo... */
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/contas", produces = MediaType.APPLICATION_JSON_VALUE)
public class ContaController {

    private final ContaService contaService;

    @GetMapping
    public List<ContaResponse> listar(@RequestParam Long pessoaId) {
        return contaService.listar(pessoaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContaResponse criar(@RequestBody ContaInput input) {
        return contaService.criar(input);
    }

    /** @param aPartirDe {@code yyyy-MM}; padrão: mês atual */
    @PutMapping("/{id}")
    public ContaResponse editar(@PathVariable UUID id, @RequestBody ContaInput input,
                                @RequestParam(required = false) String aPartirDe) {
        return contaService.editar(id, input, parseOuNulo(aPartirDe, "aPartirDe"));
    }

    /** @param aPartirDe {@code yyyy-MM}; sem ele, apaga a conta e todo o histórico */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id, @RequestParam(required = false) String aPartirDe) {
        contaService.excluir(id, parseOuNulo(aPartirDe, "aPartirDe"));
    }

    /** @param de  primeiro mês, {@code yyyy-MM}
     *  @param ate último mês, {@code yyyy-MM} */
    @GetMapping("/lancamentos")
    public List<LancamentoResponse> lancamentos(@RequestParam Long pessoaId, @RequestParam String de,
                                                @RequestParam String ate) {
        return contaService.lancamentos(pessoaId, parse(de, "de"), parse(ate, "ate"));
    }

    @PatchMapping("/lancamentos/{id}")
    public LancamentoResponse atualizarLancamento(@PathVariable UUID id, @RequestBody LancamentoInput input) {
        return contaService.atualizarLancamento(id, input);
    }
}
