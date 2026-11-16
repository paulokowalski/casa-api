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
        @Schema(description = "Maior temperatura entre os discos com SMART no período (°C)", example = "58.0") Double discoTemperaturaMax,
        @Schema(description = "Pressão de CPU (PSI some, % do tempo com tarefa esperando CPU): média e máximo") Faixa pressaoCpu,
        @Schema(description = "Pressão de memória (PSI some): média e máximo") Faixa pressaoMemoria,
        @Schema(description = "Pressão de I/O (PSI some): média e máximo") Faixa pressaoIo,
        @Schema(description = "Uso da GPU (%): média e máximo") Faixa gpu,
        @Schema(description = "Menor limite de frequência da CPU (% do máximo); abaixo de 100 houve redução por calor", example = "100.0") Double cpuLimiteMin,
        @Schema(description = "Minutos com a CPU limitada abaixo do máximo", example = "0") Long minutosCpuLimitada,
        @Schema(description = "Latência média por operação de disco (ms): média e máximo") Faixa discoLatencia,
        @Schema(description = "Maior ocupação de um disco num minuto (%)", example = "35.0") Double discoOcupacaoMax,
        @Schema(description = "Erros e pacotes descartados na rede no período", example = "0") Long redeErros) {

    public ServidorEstatisticasResponse(long amostras, Faixa cpu, Faixa memoria, Faixa temperatura, Faixa carga,
                                        Double swapMax, Double iowaitMedia, Long redeRecebidaBytes, Long redeEnviadaBytes,
                                        Long discoLidoBytes, Long discoEscritoBytes, Double discoTemperaturaMax) {
        this(amostras, cpu, memoria, temperatura, carga, swapMax, iowaitMedia, redeRecebidaBytes, redeEnviadaBytes,
                discoLidoBytes, discoEscritoBytes, discoTemperaturaMax, null, null, null, null, null, null, null, null, null);
    }

    public ServidorEstatisticasResponse comDiscoTemperaturaMax(Double valor) {
        return new ServidorEstatisticasResponse(amostras, cpu, memoria, temperatura, carga, swapMax, iowaitMedia,
                redeRecebidaBytes, redeEnviadaBytes, discoLidoBytes, discoEscritoBytes, valor, pressaoCpu, pressaoMemoria,
                pressaoIo, gpu, cpuLimiteMin, minutosCpuLimitada, discoLatencia, discoOcupacaoMax, redeErros);
    }

    @Schema(description = "Mínimo, média e máximo")
    public record Faixa(
            @Schema(example = "3.1") Double min,
            @Schema(example = "17.4") Double media,
            @Schema(example = "88.0") Double max) { }
}
