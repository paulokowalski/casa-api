package com.kowalski.casaapi.crescimento.medida;

import com.kowalski.casaapi.crescimento.medida.MedidaDtos.*;
import com.kowalski.casaapi.crescimento.oms.Indicador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/crescimento/criancas/{criancaId}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Crescimento: medidas", description = "Peso, comprimento e perímetro cefálico com os percentis da OMS")
public class MedicaoController {

    private final MedicaoService service;

    @GetMapping("/medicoes")
    @Operation(summary = "Medições", description = "Mais recentes primeiro, com escore-z, percentil e faixa da OMS.")
    public List<MedicaoResponse> listar(@PathVariable UUID criancaId) {
        return service.listar(criancaId);
    }

    @PostMapping("/medicoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar medição", description = "Só responsáveis.")
    public MedicaoResponse criar(@PathVariable UUID criancaId, @RequestBody MedicaoInput dados) {
        return service.criar(criancaId, dados);
    }

    @PutMapping("/medicoes/{id}")
    @Operation(summary = "Alterar medição", description = "Só responsáveis.")
    public MedicaoResponse atualizar(@PathVariable UUID criancaId, @PathVariable UUID id, @RequestBody MedicaoInput dados) {
        return service.atualizar(criancaId, id, dados);
    }

    @DeleteMapping("/medicoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir medição", description = "Só responsáveis.")
    public void excluir(@PathVariable UUID criancaId, @PathVariable UUID id) {
        service.excluir(criancaId, id);
    }

    @GetMapping("/resumo")
    @Operation(summary = "Resumo do crescimento", description = "Últimas medidas com percentil, ganho de peso e alertas.")
    public ResumoResponse resumo(@PathVariable UUID criancaId) {
        return service.resumo(criancaId);
    }

    @GetMapping("/curvas/{indicador}")
    @Operation(summary = "Curva de crescimento", description = "Linhas P3, P15, P50, P85 e P97 da OMS e as medidas da criança. "
            + "Para prematuros até 2 anos, os dias estão na idade corrigida.")
    public CurvaResponse curva(@PathVariable UUID criancaId, @PathVariable Indicador indicador) {
        return service.curva(criancaId, indicador);
    }
}
