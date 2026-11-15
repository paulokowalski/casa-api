package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.response.ServidorAgoraResponse;
import com.kowalski.casaapi.api.v1.response.ServidorDiaResponse;
import com.kowalski.casaapi.api.v1.response.ServidorResumoDiaResponse;
import com.kowalski.casaapi.business.monitor.MonitorService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/servidor", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Servidor", description = "Saúde do servidor: leitura ao vivo e histórico (gravado a cada minuto). Só ADMIN.")
public class ServidorController {

    private static final int DIAS_PADRAO = 30;

    private final MonitorService monitorService;

    @GetMapping("/agora")
    @Operation(summary = "Leitura ao vivo",
            description = "CPU (total e por núcleo), carga, memória, swap, temperaturas, discos, rede e I/O de disco agora.")
    @ApiResponse(responseCode = "200", description = "Leitura atual")
    @ApiResponse(responseCode = "503", description = "Não foi possível ler o sistema")
    public ServidorAgoraResponse agora() {
        return monitorService.agora();
    }

    @GetMapping("/dia")
    @Operation(summary = "Histórico de um dia",
            description = "Mínimo, média e máximo do dia, médias a cada 5 minutos e o espaço dos discos.")
    @ApiResponse(responseCode = "200", description = "Histórico do dia (vazio se não houver amostras)")
    @ApiResponse(responseCode = "503", description = "Banco do monitoramento indisponível")
    public ServidorDiaResponse dia(@Parameter(description = "Dia (aaaa-mm-dd); padrão: hoje", example = "2026-11-15")
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return monitorService.dia(data != null ? data : LocalDate.now());
    }

    @GetMapping("/dias")
    @Operation(summary = "Estatísticas diárias",
            description = "Um item por dia com amostras, do mais recente para o mais antigo. Até 366 dias por consulta.")
    @ApiResponse(responseCode = "200", description = "Estatísticas por dia")
    @ApiResponse(responseCode = "503", description = "Banco do monitoramento indisponível")
    public List<ServidorResumoDiaResponse> dias(
            @Parameter(description = "Primeiro dia; padrão: 29 dias antes de `ate`", example = "2026-10-17")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Último dia; padrão: hoje", example = "2026-11-15")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        LocalDate fim = ate != null ? ate : LocalDate.now();
        return monitorService.dias(de != null ? de : fim.minusDays(DIAS_PADRAO - 1L), fim);
    }
}
