package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Um container Docker agora. CPU, rede e disco ficam nulos quando ele está parado.")
public record ServidorContainerResponse(
        @Schema(example = "casa-api") String nome,
        @Schema(example = "casa-api") String imagem,
        @Schema(description = "Estado no Docker", example = "running") String estado,
        @Schema(description = "Situação em português", example = "ligado há 44 horas") String status,
        @Schema(description = "Healthcheck: saudavel, doente, iniciando ou nulo (sem healthcheck)", example = "saudavel") String saude,
        @Schema(description = "CPU em % do servidor inteiro (100% = todos os núcleos)", example = "1.4") Double cpuPct,
        @Schema(description = "Memória usada, sem o cache inativo (bytes)", example = "532889600") Long memUsada,
        @Schema(description = "Limite de memória (bytes; sem limite = memória do servidor)", example = "16482402304") Long memLimite,
        @Schema(description = "Memória usada em % do limite", example = "3.2") Double memPct,
        @Schema(description = "Rede recebendo (bytes/s); nulo na rede do host", example = "1200") Double redeRecebidaBps,
        @Schema(description = "Rede enviando (bytes/s); nulo na rede do host", example = "3400") Double redeEnviadaBps,
        @Schema(description = "Leitura de disco (bytes/s)", example = "0") Double discoLeituraBps,
        @Schema(description = "Escrita em disco (bytes/s)", example = "4096") Double discoEscritaBps,
        @Schema(description = "Processos e threads", example = "51") Integer processos) { }
