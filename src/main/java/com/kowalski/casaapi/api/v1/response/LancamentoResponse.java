package com.kowalski.casaapi.api.v1.response;

import com.kowalski.casaapi.business.enumerator.TipoConta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LancamentoResponse(
        UUID id,
        UUID contaId,
        String descricao,
        String categoria,
        TipoConta tipo,
        String competencia,
        LocalDate vencimento,
        Integer numeroParcela,
        Integer totalParcelas,
        BigDecimal valor,
        boolean valorEstimado,
        boolean paga,
        LocalDate dataPagamento,
        boolean debitoAutomatico,
        boolean valorVariavel) {
}
