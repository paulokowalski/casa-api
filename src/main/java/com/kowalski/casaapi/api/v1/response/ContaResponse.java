package com.kowalski.casaapi.api.v1.response;

import com.kowalski.casaapi.business.enumerator.TipoConta;

import java.math.BigDecimal;
import java.util.UUID;

public record ContaResponse(
        UUID id,
        Long pessoaId,
        UUID categoriaId,
        String categoria,
        String descricao,
        TipoConta tipo,
        BigDecimal valor,
        Integer diaVencimento,
        String competenciaInicio,
        String competenciaFim,
        Integer parcelaInicial,
        Integer totalParcelas,
        BigDecimal jurosMes,
        boolean valorVariavel,
        boolean debitoAutomatico,
        boolean ativa,
        Integer parcelaAtual,
        Integer parcelasRestantes,
        BigDecimal saldoRestante) {
}
