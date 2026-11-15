package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.kowalski.casaapi.business.enumerator.TipoConta;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Conta com a situação calculada para o mês atual")
public record ContaResponse(
        @Schema(description = "Id da conta") UUID id,
        @Schema(description = "Id da pessoa", example = "1") Long pessoaId,
        @Schema(description = "Id da categoria") UUID categoriaId,
        @Schema(description = "Nome da categoria", example = "Casa") String categoria,
        @Schema(description = "Descrição", example = "Internet") String descricao,
        @Schema(description = "Tipo da conta", example = "RECORRENTE") TipoConta tipo,
        @Schema(description = "Valor mensal", example = "129.90") BigDecimal valor,
        @Schema(description = "Dia do vencimento", example = "10") Integer diaVencimento,
        @Schema(description = "Primeiro mês (aaaa-mm)", example = "2026-01") String competenciaInicio,
        @Schema(description = "Último mês (aaaa-mm); nulo = sem fim") String competenciaFim,
        @Schema(description = "PARCELADA: parcela do primeiro mês", example = "1") Integer parcelaInicial,
        @Schema(description = "PARCELADA: total de parcelas", example = "12") Integer totalParcelas,
        @Schema(description = "PARCELADA: juros ao mês em %", example = "1.99") BigDecimal jurosMes,
        @Schema(description = "Valor muda todo mês", example = "false") boolean valorVariavel,
        @Schema(description = "Em débito automático", example = "false") boolean debitoAutomatico,
        @Schema(description = "Ainda tem lançamentos a partir do mês atual", example = "true") boolean ativa,
        @Schema(description = "PARCELADA: número da parcela no mês atual", example = "4") Integer parcelaAtual,
        @Schema(description = "PARCELADA: parcelas que faltam depois da atual", example = "8") Integer parcelasRestantes,
        @Schema(description = "PARCELADA: valor × parcelas restantes", example = "1039.20") BigDecimal saldoRestante) {
}
