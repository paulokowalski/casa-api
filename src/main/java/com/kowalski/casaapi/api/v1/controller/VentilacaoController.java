package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.response.VentilacaoResponse;
import com.kowalski.casaapi.business.monitor.VentilacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = VentilacaoController.CAMINHO, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Servidor", description = "Saúde do servidor: leitura ao vivo e histórico (gravado a cada minuto). Só ADMIN.")
public class VentilacaoController {

    public static final String CAMINHO = "/v1/servidor/ventilacao";
    private static final int DIAS_PADRAO = 30;

    private final VentilacaoService ventilacaoService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Gravar leitura da ventilação",
            description = "Enviado pelo Home Assistant (X-API-KEY) a cada minuto e quando a tomada das ventoinhas muda. "
                    + "Uma linha por minuto: outra leitura no mesmo minuto substitui a anterior.")
    @ApiResponse(responseCode = "204", description = "Gravada")
    @ApiResponse(responseCode = "400", description = "Estado da tomada inválido")
    @ApiResponse(responseCode = "503", description = "Banco do monitoramento indisponível")
    public void registrar(@RequestBody VentilacaoResponse.Leitura leitura) {
        ventilacaoService.registrar(leitura);
    }

    @GetMapping
    @Operation(summary = "Ventilação agora",
            description = "Última leitura do HA (tomada, temperaturas, limites), desde quando a tomada está assim e o "
                    + "tempo ligada hoje. `desatualizado` = sem leitura há mais de 3 minutos.")
    @ApiResponse(responseCode = "200", description = "Leitura atual")
    @ApiResponse(responseCode = "503", description = "Sem leituras ou banco indisponível")
    public VentilacaoResponse.Agora agora() {
        return ventilacaoService.agora();
    }

    @GetMapping("/dia")
    @Operation(summary = "Ventilação num dia", description = "Resumo, períodos ligada e um ponto por minuto (tomada e temperaturas).")
    @ApiResponse(responseCode = "200", description = "Histórico do dia")
    @ApiResponse(responseCode = "503", description = "Banco do monitoramento indisponível")
    public VentilacaoResponse.Dia dia(@Parameter(description = "Dia (aaaa-mm-dd); padrão: hoje", example = "2026-11-21")
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ventilacaoService.dia(data != null ? data : LocalDate.now());
    }

    @GetMapping("/dias")
    @Operation(summary = "Ventilação por dia", description = "Tempo ligada por dia (só dias com leitura), do mais recente ao mais antigo.")
    @ApiResponse(responseCode = "200", description = "Resumo por dia")
    @ApiResponse(responseCode = "503", description = "Banco do monitoramento indisponível")
    public List<VentilacaoResponse.ResumoDia> dias(
            @Parameter(description = "Primeiro dia; padrão: 29 dias antes de `ate`") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Último dia; padrão: hoje") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        LocalDate fim = ate != null ? ate : LocalDate.now();
        return ventilacaoService.dias(de != null ? de : fim.minusDays(DIAS_PADRAO - 1L), fim);
    }
}
