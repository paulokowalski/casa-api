package com.kowalski.casaapi.despesa;

import com.kowalski.casaapi.pessoa.UsuarioLogado;
import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/despesas", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Despesas", description = "Totais de gastos no cartão por mês")
public class DespesaController {

    private final DespesaService despesaService;
    private final UsuarioLogado usuarioLogado;

    @GetMapping("/{ano}")
    @Operation(summary = "Totais do cartão no ano",
            description = "Para cada mês (01 a 12): total das parcelas, quanto é de compras parceladas e quanto é de compras à vista.")
    @ApiResponse(responseCode = "200", description = "Totais por mês")
    @ApiResponse(responseCode = "404", description = "Nenhuma despesa no ano")
    public DespesaAnualResponse doAno(@Parameter(description = "Ano", example = "2026") @PathVariable String ano,
                                      @Parameter(description = "Nome da pessoa", example = "PAULO") @RequestParam String pessoa) {
        DespesaAnualResponse r = despesaService.buscarDespesasAnuais(ano, usuarioLogado.nomePessoa(pessoa));
        if (r == null || r.despesasPorMes().isEmpty()) throw new RecursoNaoEncontradoException("Nenhuma despesa no ano");
        return r;
    }
}
