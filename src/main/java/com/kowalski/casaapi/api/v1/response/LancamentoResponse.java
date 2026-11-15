package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.kowalski.casaapi.business.enumerator.TipoConta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Lançamento de uma conta num mês")
public record LancamentoResponse(
        @Schema(description = "Id do lançamento") UUID id,
        @Schema(description = "Id da conta") UUID contaId,
        @Schema(description = "Descrição da conta", example = "Internet") String descricao,
        @Schema(description = "Categoria da conta", example = "Casa") String categoria,
        @Schema(description = "Tipo da conta", example = "RECORRENTE") TipoConta tipo,
        @Schema(description = "Mês no formato aaaa-mm", example = "2026-11") String competencia,
        @Schema(description = "Data de vencimento", example = "2026-11-10") LocalDate vencimento,
        @Schema(description = "PARCELADA: número da parcela", example = "4") Integer numeroParcela,
        @Schema(description = "PARCELADA: total de parcelas", example = "12") Integer totalParcelas,
        @Schema(description = "Valor do mês", example = "129.90") BigDecimal valor,
        @Schema(description = "O valor é estimativa (conta variável ainda sem o valor real)", example = "false") boolean valorEstimado,
        @Schema(description = "Pago", example = "false") boolean paga,
        @Schema(description = "Data do pagamento") LocalDate dataPagamento,
        @Schema(description = "A conta é em débito automático", example = "false") boolean debitoAutomatico,
        @Schema(description = "A conta tem valor variável", example = "false") boolean valorVariavel) {
}
