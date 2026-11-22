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
import java.util.Map;
import com.kowalski.casaapi.api.v1.response.ServidorEventoResponse;

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

    @GetMapping("/host")
    @Operation(summary = "Status do host",
            description = "Backup (3 cópias), serviços e timers, montagens, inodes, alertas do monitor, erros do kernel, "
                    + "atualizações (apt e Diun), internet/DNS/Tailscale, logins SSH, NPU e Docker (reinícios e espaço). "
                    + "Gravado pelo host a cada minuto; `desatualizado` = arquivo com mais de 5 minutos.")
    @ApiResponse(responseCode = "200", description = "Status atual")
    @ApiResponse(responseCode = "503", description = "Arquivo do host ausente ou desligado")
    public Map<String, Object> host() {
        return monitorService.host();
    }

    @GetMapping("/eventos")
    @Operation(summary = "Eventos do servidor",
            description = "Logins SSH, backups, alertas, quedas de internet, discos, containers que reiniciaram, acessos pela VPN (Tailscale), kernel e boots. "
                    + "Mais recentes primeiro, até 500.")
    @ApiResponse(responseCode = "200", description = "Eventos do período")
    @ApiResponse(responseCode = "503", description = "Banco do monitoramento indisponível")
    public List<ServidorEventoResponse> eventos(
            @Parameter(description = "Primeiro dia; padrão: 6 dias antes de `ate`", example = "2026-11-09")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Último dia; padrão: hoje", example = "2026-11-15")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        LocalDate fim = ate != null ? ate : LocalDate.now();
        return monitorService.eventos(de != null ? de : fim.minusDays(6), fim);
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
