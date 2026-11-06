package com.kowalski.casaapi.api.v1.input;

import com.kowalski.casaapi.business.enumerator.TipoConta;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cadastro/edição de conta fora do cartão.
 *
 * @param competenciaInicio primeiro mês, formato {@code yyyy-MM}
 * @param competenciaFim    último mês ({@code yyyy-MM}); só para RECORRENTE com data para acabar
 * @param parcelaInicial    parcela do primeiro mês (PARCELADA já em andamento); padrão 1
 * @param jurosMes          juros ao mês em % (1.49 = 1,49% a.m.), opcional
 */
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
