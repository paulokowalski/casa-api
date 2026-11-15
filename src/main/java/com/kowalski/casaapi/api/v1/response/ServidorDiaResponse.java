package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Histórico de um dia: resumo, série a cada 5 minutos e espaço dos discos")
public record ServidorDiaResponse(
        @Schema(description = "Dia", example = "2026-11-15") LocalDate data,
        @Schema(description = "Estatísticas do dia") ServidorEstatisticasResponse resumo,
        @Schema(description = "Médias a cada 5 minutos (só os blocos com amostras)") List<Ponto> serie,
        @Schema(description = "Última medição do dia de cada disco") List<ServidorDiscoResponse> discos,
        @Schema(description = "Resumo do dia de cada container, os que mais usaram CPU primeiro") List<ContainerDia> containers,
        @Schema(description = "CPU e memória de cada container a cada 5 minutos (só enquanto rodando)") List<SerieContainer> serieContainers,
        @Schema(description = "Temperatura de cada disco a cada 5 minutos (só discos com SMART, fora do repouso)") List<SerieDisco> serieDiscos) {

    @Schema(description = "Temperatura de um disco ao longo do dia")
    public record SerieDisco(
            @Schema(example = "Mídia") String nome,
            List<PontoDisco> pontos) { }

    public record PontoDisco(
            @Schema(description = "Início do bloco (HH:mm)", example = "14:35") String horario,
            @Schema(description = "Temperatura (°C)", example = "56.0") Double temperatura) { }

    @Schema(description = "Um container ao longo do dia")
    public record ContainerDia(
            @Schema(example = "jellyfin") String nome,
            @Schema(description = "Imagem na última amostra", example = "linuxserver/jellyfin:latest") String imagem,
            @Schema(description = "Estado na última amostra", example = "running") String estado,
            @Schema(description = "Situação na última amostra", example = "ligado há 44 horas") String status,
            @Schema(description = "Healthcheck na última amostra", example = "saudavel") String saude,
            @Schema(description = "Minutos com amostra no dia", example = "1440") long minutos,
            @Schema(description = "Minutos rodando no dia", example = "1440") long minutosRodando,
            @Schema(description = "CPU média (% do servidor)", example = "2.1") Double cpuMedia,
            @Schema(description = "CPU máxima (% do servidor)", example = "35.0") Double cpuMax,
            @Schema(description = "Memória média (bytes)", example = "900000000") Long memMedia,
            @Schema(description = "Memória máxima (bytes)", example = "1200000000") Long memMax,
            @Schema(description = "Bytes recebidos pela rede no dia", example = "15595616") Long redeRecebidaBytes,
            @Schema(description = "Bytes enviados pela rede no dia", example = "2082874275") Long redeEnviadaBytes,
            @Schema(description = "Bytes lidos do disco no dia", example = "3497443328") Long discoLidoBytes,
            @Schema(description = "Bytes escritos no disco no dia", example = "15233024") Long discoEscritoBytes) { }

    @Schema(description = "Série de um container")
    public record SerieContainer(
            @Schema(example = "jellyfin") String nome,
            List<PontoContainer> pontos) { }

    public record PontoContainer(
            @Schema(description = "Início do bloco (HH:mm)", example = "14:35") String horario,
            @Schema(description = "CPU média (% do servidor)", example = "2.5") Double cpu,
            @Schema(description = "Memória média (bytes)", example = "900000000") Long memoria) { }


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
