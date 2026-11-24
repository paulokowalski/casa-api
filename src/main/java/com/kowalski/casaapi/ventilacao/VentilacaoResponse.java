package com.kowalski.casaapi.ventilacao;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class VentilacaoResponse {

    private VentilacaoResponse() {
    }

    public record Limite(Double liga, Double desliga) {
    }

    @Schema(description = "Leitura que o Home Assistant envia a cada minuto e quando a tomada muda")
    public record Leitura(
            @Schema(description = "Estado da tomada das ventoinhas no HA: on, off, unavailable ou unknown", example = "on")
            String tomada,
            @Schema(description = "binary_sensor.servidor_precisa_ventilacao ligado") Boolean precisa,
            @Schema(description = "Controle automático ligado (input_boolean.ventilacao_servidor_automatica)") Boolean automatica,
            @Schema(description = "Maior temperatura do SoC, °C") Double soc,
            @Schema(description = "HD mais quente, °C (0 = todos em repouso)") Double hds,
            @Schema(description = "NVMe, °C") Double nvme,
            Limite limiteSoc,
            Limite limiteHds,
            Limite limiteNvme) {
    }

    @Schema(description = "Situação agora")
    public record Agora(
            @Schema(description = "Quando chegou a última leitura do HA") LocalDateTime recebidoEm,
            @Schema(description = "Última leitura com mais de 3 minutos") boolean desatualizado,
            Leitura leitura,
            @Schema(description = "Desde quando a tomada está no estado atual (dentro do que foi gravado)") LocalDateTime desde,
            @Schema(description = "Minutos com a tomada ligada hoje") long minutosLigadaHoje,
            @Schema(description = "Minutos com leitura hoje") long minutosComDadosHoje) {
    }

    @Schema(description = "Um minuto do dia")
    public record Ponto(String horario, Integer ligada, Double soc, Double hds, Double nvme) {
    }

    @Schema(description = "Período contínuo com a tomada ligada")
    public record Periodo(LocalDateTime inicio, LocalDateTime fim, long minutos) {
    }

    public record ResumoDia(
            LocalDate data,
            @Schema(description = "Minutos com a tomada ligada") long minutosLigada,
            @Schema(description = "Minutos com leitura do HA (o resto do dia ficou sem dados)") long minutosComDados,
            @Schema(description = "Quantas vezes ligou") long vezesLigou,
            Double maxSoc, Double maxHds, Double maxNvme) {
    }

    public record Dia(ResumoDia resumo, List<Periodo> periodos, List<Ponto> pontos) {
    }
}
