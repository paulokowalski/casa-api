package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.input.FaturaInput;
import com.kowalski.casaapi.api.v1.response.FaturaResponse;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.business.service.FaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static com.kowalski.casaapi.comum.Competencias.de;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/faturas", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Faturas", description = "Situação de pagamento da fatura do cartão em cada mês")
public class FaturaController {

    private final FaturaService faturaService;
    private final UsuarioLogado usuarioLogado;

    @GetMapping("/{ano}/{mes}")
    @Operation(summary = "Situação da fatura no mês", description = "Se não houver registro, vem como não paga.")
    public FaturaResponse obter(@Parameter(description = "Ano", example = "2026") @PathVariable int ano,
                                @Parameter(description = "Mês (1 a 12)", example = "11") @PathVariable int mes,
                                @Parameter(description = "Id da pessoa", example = "1") @RequestParam Long pessoaId) {
        return faturaService.obter(usuarioLogado.pessoaId(pessoaId), de(ano, mes));
    }

    @PutMapping("/{ano}/{mes}")
    @Operation(summary = "Marcar fatura como paga ou não paga")
    public FaturaResponse salvar(@Parameter(description = "Ano", example = "2026") @PathVariable int ano,
                                 @Parameter(description = "Mês (1 a 12)", example = "11") @PathVariable int mes,
                                 @Parameter(description = "Id da pessoa", example = "1") @RequestParam Long pessoaId,
                                 @RequestBody FaturaInput input) {
        return faturaService.salvar(usuarioLogado.pessoaId(pessoaId), de(ano, mes), input);
    }
}
