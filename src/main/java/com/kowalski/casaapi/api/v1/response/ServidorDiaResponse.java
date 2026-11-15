package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Histórico de um dia: resumo, série a cada 5 minutos e espaço dos discos")
public record ServidorDiaResponse(
        @Schema(description = "Dia", example = "2026-11-15") LocalDate data,
        @Schema(description = "Estatísticas do dia") ServidorEstatisticasResponse resumo,
        @Schema(description = "Médias a cada 5 minutos (só os blocos com amostras)") List<Ponto> serie,
        @Schema(description = "Última medição do dia de cada disco") List<ServidorDiscoResponse> discos) {

    @Schema(description = "Médias de um bloco de 5 minutos")
    public record Ponto(
            @Schema(description = "Início do bloco (HH:mm)", example = "14:35") String horario,
            @Schema(description = "CPU média (%)", example = "12.5") double cpu,
            @Schema(description = "CPU máxima no bloco (%)", example = "40.2") double cpuMax,
            @Schema(description = "Memória (%)", example = "28.1") double memoria,
            @Schema(description = "Swap (%)", example = "5.2") double swap,
            @Schema(description = "Maior temperatura, média do bloco (°C)", example = "54.6") Double temperatura,
            @Schema(description = "Maior temperatura do bloco (°C)", example = "56.0") Double temperaturaMax,
            @Schema(description = "Carga de 1 minuto", example = "1.8") double carga,
            @Schema(description = "CPU esperando disco (%)", example = "0.3") double iowait,
            @Schema(description = "Rede recebendo (bytes/s)", example = "15000") double redeRecebidaBps,
            @Schema(description = "Rede enviando (bytes/s)", example = "220000") double redeEnviadaBps,
            @Schema(description = "Leitura de disco (bytes/s)", example = "100000") double discoLeituraBps,
            @Schema(description = "Escrita em disco (bytes/s)", example = "45000") double discoEscritaBps) { }
}
