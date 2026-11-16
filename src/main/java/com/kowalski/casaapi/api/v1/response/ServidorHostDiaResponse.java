package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Histórico do status do host (amostra_host, uma por minuto). */
public final class ServidorHostDiaResponse {

    private ServidorHostDiaResponse() {
    }

    @Schema(description = "Resumo do host num período")
    public record Resumo(
            @Schema(description = "Minutos com amostra", example = "1440") long amostras,
            @Schema(description = "Minutos sem internet", example = "0") long minutosSemInternet,
            @Schema(description = "Latência até a internet (ms)") ServidorEstatisticasResponse.Faixa internetLatencia,
            @Schema(description = "Latência média até o roteador (ms)", example = "1.0") Double gatewayLatenciaMedia,
            @Schema(description = "Minutos com o DNS (Pi-hole) falhando", example = "0") long minutosDnsFalhando,
            @Schema(description = "Tempo médio de resposta do DNS (ms)", example = "40") Double dnsTempoMedio,
            @Schema(description = "Minutos com o Tailscale offline", example = "0") long minutosTailscaleOffline,
            @Schema(description = "Uso médio e máximo da NPU (%)") ServidorEstatisticasResponse.Faixa npu,
            @Schema(description = "Máximo de alertas ativos ao mesmo tempo", example = "0") long alertasMax,
            @Schema(description = "Minutos com algum alerta ativo", example = "0") long minutosComAlerta,
            @Schema(description = "Máximo de serviços com falha ao mesmo tempo", example = "0") long servicosFalhosMax,
            @Schema(description = "Minutos com algum disco desmontado", example = "0") long minutosDiscoDesmontado,
            @Schema(description = "Maior idade do backup no período (horas)", example = "24.5") Double backupIdadeMaxHoras,
            @Schema(description = "Reinícios de containers no período", example = "0") Long containersReinicios,
            @Schema(description = "Pacotes pendentes na última amostra", example = "2") Long aptPendentes,
            @Schema(description = "Reinício necessário na última amostra") Boolean reinicioNecessario,
            @Schema(description = "Eventos registrados no período", example = "5") long eventos,
            @Schema(description = "Eventos de erro no período", example = "0") long eventosErro) { }

    @Schema(description = "Médias do host num bloco de 5 minutos")
    public record Ponto(
            @Schema(description = "Início do bloco (HH:mm)", example = "14:35") String horario,
            @Schema(description = "Latência média até a internet (ms)", example = "4.1") Double internetMs,
            @Schema(description = "Maior latência no bloco (ms)", example = "9.0") Double internetMaxMs,
            @Schema(description = "Latência média até o roteador (ms)", example = "1.0") Double gatewayMs,
            @Schema(description = "Tempo médio do DNS (ms)", example = "35") Double dnsMs,
            @Schema(description = "% do bloco sem internet", example = "0") double semInternetPct,
            @Schema(description = "Uso médio da NPU (%)", example = "0") Double npu) { }
}
