package com.kowalski.casaapi.despesa;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Totais do cartão por mês no ano")
public record DespesaAnualResponse(
        @Schema(description = "Pessoa", example = "PAULO") String pessoa,
        @Schema(description = "Ano", example = "2026") String ano,
        @Schema(description = "Totais por mês, com chave \"01\" a \"12\"") Map<String, DespesaResponse> despesasPorMes) { } 