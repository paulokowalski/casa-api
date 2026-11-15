package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Leitura atual do servidor (CPU e taxas medidas em meio segundo)")
public record ServidorAgoraResponse(
        @Schema(description = "Momento da leitura") LocalDateTime lidoEm,
        @Schema(description = "Nome do host", example = "orangepi5plus") String host,
        @Schema(description = "Modelo da placa, quando o kernel informa", example = "Orange Pi 5 Plus") String modelo,
        @Schema(description = "Versão do kernel", example = "5.10.160-rockchip-rk3588") String kernel,
        @Schema(description = "Tempo ligado (segundos)", example = "156239") long uptimeSegundos,
        Cpu cpu,
        Carga carga,
        Memoria memoria,
        @Schema(description = "Sensores de temperatura") List<Temperatura> temperaturas,
        @Schema(description = "Maior temperatura entre os sensores (°C)", example = "55.0") Double temperaturaMax,
        @Schema(description = "Espaço dos discos") List<ServidorDiscoResponse> discos,
        @Schema(description = "Tráfego das interfaces físicas") List<Rede> rede,
        @Schema(description = "Leitura e escrita somando os discos") DiscoIo discoIo,
        @Schema(description = "Containers Docker, os que mais usam CPU primeiro (vazio se o monitoramento de containers estiver desligado)")
        List<ServidorContainerResponse> containers) {

    @Schema(description = "Uso de CPU")
    public record Cpu(
            @Schema(description = "Uso total (%)", example = "16.6") double usoPct,
            @Schema(description = "Tempo esperando disco (%)", example = "0.2") double iowaitPct,
            @Schema(description = "Uso e frequência de cada núcleo") List<Nucleo> nucleos) { }

    public record Nucleo(
            @Schema(example = "0") int nucleo,
            @Schema(description = "Uso (%)", example = "29.4") double usoPct,
            @Schema(description = "Frequência atual (MHz)", example = "1800") Long mhz) { }

    @Schema(description = "Carga média (load average) e processos")
    public record Carga(
            @Schema(example = "1.11") double min1,
            @Schema(example = "1.60") double min5,
            @Schema(example = "1.83") double min15,
            @Schema(description = "Processos e threads existentes", example = "1070") int processos) { }

    @Schema(description = "Memória e swap (bytes)")
    public record Memoria(
            @Schema(example = "16482402304") long total,
            @Schema(description = "Total menos o disponível (sem cache)", example = "4655173632") long usada,
            @Schema(example = "28.2") double pct,
            @Schema(example = "8241197056") long swapTotal,
            @Schema(example = "427294720") long swapUsada,
            @Schema(example = "5.2") double swapPct) { }

    public record Temperatura(
            @Schema(example = "soc") String sensor,
            @Schema(example = "54.5") double celsius) { }

    public record Rede(
            @Schema(example = "enP4p65s0") String interfaceRede,
            @Schema(description = "Recebendo (bytes/s)", example = "15000") double recebidaBps,
            @Schema(description = "Enviando (bytes/s)", example = "220000") double enviadaBps) { }

    public record DiscoIo(
            @Schema(description = "Leitura (bytes/s)", example = "100000") double leituraBps,
            @Schema(description = "Escrita (bytes/s)", example = "45000") double escritaBps) { }
}
