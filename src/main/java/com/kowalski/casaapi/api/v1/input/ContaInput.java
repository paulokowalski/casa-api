package com.kowalski.casaapi.api.v1.input;

import com.kowalski.casaapi.business.enumerator.TipoConta;

import java.math.BigDecimal;
import java.util.UUID;

public record ContaInput(
        Long pessoaId,
        UUID categoriaId,
        String descricao,
        TipoConta tipo,
        BigDecimal valor,
        Integer diaVencimento,
        String competenciaInicio,
        String competenciaFim,
        Integer parcelaInicial,
        Integer totalParcelas,
        BigDecimal jurosMes,
        Boolean valorVariavel,
        Boolean debitoAutomatico) {
}
