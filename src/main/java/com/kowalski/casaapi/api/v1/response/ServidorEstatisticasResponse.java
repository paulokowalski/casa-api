package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estatísticas de um período (mínimo, média e máximo das amostras de 1 minuto)")
public record ServidorEstatisticasResponse(
        @Schema(description = "Quantidade de amostras no período (1 por minuto)", example = "1440") long amostras,
        @Schema(description = "Uso de CPU (%)") Faixa cpu,
        @Schema(description = "Uso de memória (%)") Faixa memoria,
        @Schema(description = "Maior temperatura entre os sensores (°C)") Faixa temperatura,
        @Schema(description = "Carga média de 1 minuto (load average)") Faixa carga,
        @Schema(description = "Maior uso de swap (%)", example = "5.2") Double swapMax,
        @Schema(description = "Média do tempo de CPU esperando disco (%)", example = "0.4") Double iowaitMedia,
        @Schema(description = "Bytes recebidos pela rede no período", example = "1073741824") Long redeRecebidaBytes,
        @Schema(description = "Bytes enviados pela rede no período", example = "536870912") Long redeEnviadaBytes,
        @Schema(description = "Bytes lidos dos discos no período", example = "2147483648") Long discoLidoBytes,
        @Schema(description = "Bytes escritos nos discos no período", example = "1073741824") Long discoEscritoBytes,
        @Schema(description = "Maior temperatura entre os discos com SMART no período (°C)", example = "58.0") Double discoTemperaturaMax) {

    public ServidorEstatisticasResponse comDiscoTemperaturaMax(Double valor) {
        return new ServidorEstatisticasResponse(amostras, cpu, memoria, temperatura, carga, swapMax, iowaitMedia,
                redeRecebidaBytes, redeEnviadaBytes, discoLidoBytes, discoEscritoBytes, valor);
    }

    @Schema(description = "Mínimo, média e máximo")
    public record Faixa(
            @Schema(example = "3.1") Double min,
            @Schema(example = "17.4") Double media,
            @Schema(example = "88.0") Double max) { }
}
