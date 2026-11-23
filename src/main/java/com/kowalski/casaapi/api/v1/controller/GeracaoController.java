package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.dto.GeradoDTO;
import com.kowalski.casaapi.api.v1.response.GeracaoDiariaResponse;
import com.kowalski.casaapi.api.v1.response.GeracaoMensalResponse;
import com.kowalski.casaapi.api.v1.response.GeracaoMesAnoResponse;
import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.service.GeracaoService;
import com.kowalski.casaapi.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/geracao-solar", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Geração solar", description = "Leituras do inversor solar: potência instantânea e energia gerada")
public class GeracaoController {

    private final GeracaoService geracaoService;

    private static <T> T encontrado(T valor) {
        if (valor == null) throw new RecursoNaoEncontradoException("Sem dados de geração");
        return valor;
    }

    @GetMapping
    @Operation(summary = "Geração de um dia", description = "Leituras de potência ao longo do dia e a energia total gerada (kWh).")
    @ApiResponse(responseCode = "200", description = "Leituras do dia")
    @ApiResponse(responseCode = "404", description = "Sem dados de geração")
    public GeracaoDiariaResponse doDia(@Parameter(description = "Dia (aaaa-mm-dd)", example = "2026-11-15")
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return encontrado(geracaoService.buscarGeracao(data));
    }

    @GetMapping("/{ano}/{mes}")
    @Operation(summary = "Geração por dia no mês", description = "Energia gerada (kWh) em cada dia do mês.")
    @ApiResponse(responseCode = "200", description = "Geração por dia")
    @ApiResponse(responseCode = "404", description = "Sem dados de geração")
    public GeracaoMensalResponse doMes(@Parameter(description = "Ano", example = "2026") @PathVariable int ano,
                                       @Parameter(description = "Mês (1 a 12)", example = "11") @PathVariable int mes) {
        return encontrado(geracaoService.buscarGeracaoMensal(ano, mes));
    }

    @GetMapping("/{ano}")
    @Operation(summary = "Geração por mês no ano", description = "Energia gerada (kWh) em cada mês do ano.")
    @ApiResponse(responseCode = "200", description = "Geração por mês")
    @ApiResponse(responseCode = "404", description = "Sem dados de geração")
    public List<GeracaoMesAnoResponse> doAno(@Parameter(description = "Ano", example = "2026") @PathVariable int ano) {
        return encontrado(geracaoService.buscarGeracaoAnual(ano));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrar leitura do inversor",
            description = "Chamado pela integração (Node-RED) a cada leitura. Aceita a chave `X-API-KEY` ou token de admin.",
            security = {@SecurityRequirement(name = OpenApiConfig.API_KEY), @SecurityRequirement(name = OpenApiConfig.JWT)})
    @ApiResponse(responseCode = "200", description = "Leitura registrada")
    public void salvar(@RequestBody GeradoDTO dto) {
        geracaoService.salvar(dto);
    }
}
