package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Espaço e temperatura de um disco")
public record ServidorDiscoResponse(
        @Schema(description = "Nome configurado em monitor.discos", example = "NVMe") String nome,
        @Schema(description = "Caminho medido dentro do container", example = "/host/discos/nvme") String caminho,
        @Schema(description = "Tamanho total (bytes)", example = "1006979764224") long total,
        @Schema(description = "Espaço usado (bytes)", example = "106906300416") long usado,
        @Schema(description = "Percentual usado, como no df", example = "10.6") double pct,
        @Schema(description = "Modelo informado pelo SMART", example = "WDC WD30PURX-64P6ZY0") String modelo,
        @Schema(description = "nvme, ssd ou hdd (do SMART); nulo sem SMART", example = "hdd") String tipo,
        @Schema(description = "Temperatura (°C); nula se o disco está em repouso ou não tem SMART", example = "56.0") Double temperatura,
        @Schema(description = "ativo, repouso (disco parado, não foi acordado) ou sem_dados", example = "ativo") String estadoTemperatura,
        @Schema(description = "Só no histórico: temperatura média do dia (°C)", example = "54.2") Double temperaturaMedia,
        @Schema(description = "Só no histórico: temperatura máxima do dia (°C)", example = "58.0") Double temperaturaMax) { }
