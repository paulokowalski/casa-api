package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.response.InternetResponse;
import com.kowalski.casaapi.business.monitor.InternetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/servidor/internet", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Servidor", description = "Saúde do servidor: leitura ao vivo e histórico (gravado a cada minuto). Só ADMIN.")
public class InternetController {

    private static final int DIAS_PADRAO = 30;

    private final InternetService internetService;

    @GetMapping
    @Operation(summary = "Internet agora",
            description = "Ping (média, jitter, perda) ao roteador, modem, Cloudflare e Google; DNS pelo Pi-hole e direto; "
                    + "IP público e provedor; Pi-hole; interface de rede e o último teste de velocidade. "
                    + "Gravado pelo host a cada minuto; `desatualizado` = arquivo com mais de 3 minutos.")
    @ApiResponse(responseCode = "200", description = "Leitura atual")
    @ApiResponse(responseCode = "503", description = "Arquivo da internet ausente")
    public Map<String, Object> agora() {
        return internetService.agora();
    }

    @GetMapping("/dia")
    @Operation(summary = "Internet num dia", description = "Resumo, médias a cada 5 minutos e testes de velocidade do dia.")
    @ApiResponse(responseCode = "200", description = "Histórico do dia")
    @ApiResponse(responseCode = "503", description = "Banco do monitoramento indisponível")
    public InternetResponse.Dia dia(@Parameter(description = "Dia (aaaa-mm-dd); padrão: hoje", example = "2026-11-21")
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return internetService.dia(data != null ? data : LocalDate.now());
    }

    @GetMapping("/dias")
    @Operation(summary = "Internet por dia", description = "Resumo de cada dia com amostras, do mais recente ao mais antigo.")
    @ApiResponse(responseCode = "200", description = "Resumo por dia")
    @ApiResponse(responseCode = "503", description = "Banco do monitoramento indisponível")
    public List<InternetResponse.ResumoDia> dias(
            @Parameter(description = "Primeiro dia; padrão: 29 dias antes de `ate`") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Último dia; padrão: hoje") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        LocalDate fim = ate != null ? ate : LocalDate.now();
        return internetService.dias(de != null ? de : fim.minusDays(DIAS_PADRAO - 1L), fim);
    }

    @GetMapping("/velocidade")
    @Operation(summary = "Testes de velocidade", description = "Testes do período, do mais antigo ao mais recente.")
    @ApiResponse(responseCode = "200", description = "Testes")
    @ApiResponse(responseCode = "503", description = "Banco do monitoramento indisponível")
    public List<InternetResponse.TesteVelocidade> velocidade(
            @Parameter(description = "Primeiro dia; padrão: 29 dias antes de `ate`") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Último dia; padrão: hoje") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        LocalDate fim = ate != null ? ate : LocalDate.now();
        return internetService.velocidade(de != null ? de : fim.minusDays(DIAS_PADRAO - 1L), fim);
    }
}
