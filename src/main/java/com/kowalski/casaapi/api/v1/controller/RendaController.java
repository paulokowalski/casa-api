package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.input.RendaInput;
import com.kowalski.casaapi.api.v1.response.RendaResponse;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.business.service.RendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static com.kowalski.casaapi.util.Competencias.de;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/rendas", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Renda", description = "Renda mensal da pessoa")
public class RendaController {

    private final RendaService rendaService;
    private final UsuarioLogado usuarioLogado;

    @GetMapping("/{ano}/{mes}")
    @Operation(summary = "Renda do mês",
            description = "Sem valor cadastrado no mês, vale o do último mês anterior que tiver (`herdada = true`, "
                    + "`origem` = mês de onde veio). Sem nenhum, `valor` vem nulo.")
    public RendaResponse obter(@Parameter(description = "Ano", example = "2026") @PathVariable int ano,
                               @Parameter(description = "Mês (1 a 12)", example = "11") @PathVariable int mes,
                               @Parameter(description = "Id da pessoa", example = "1") @RequestParam Long pessoaId) {
        return rendaService.obter(usuarioLogado.pessoaId(pessoaId), de(ano, mes));
    }

    @PutMapping("/{ano}/{mes}")
    @Operation(summary = "Definir renda a partir do mês",
            description = "O valor vale para este mês e os seguintes, até outro mês ter valor próprio. "
                    + "Valor vazio ou zero remove o valor do mês (volta a herdar do anterior).")
    @ApiResponse(responseCode = "200", description = "Renda salva")
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
    public RendaResponse salvar(@Parameter(description = "Ano", example = "2026") @PathVariable int ano,
                                @Parameter(description = "Mês (1 a 12)", example = "11") @PathVariable int mes,
                                @Parameter(description = "Id da pessoa", example = "1") @RequestParam Long pessoaId,
                                @RequestBody RendaInput input) {
        return rendaService.salvar(usuarioLogado.pessoaId(pessoaId), de(ano, mes), input);
    }
}
