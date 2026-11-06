package com.kowalski.casaapi.api.v1.response;

import java.math.BigDecimal;

/**
 * Renda vigente no mês.
 *
 * @param competencia     mês pedido ({@code yyyy-MM})
 * @param origem          mês de onde veio o valor ({@code yyyy-MM}); diferente de {@code competencia} quando herdado
 * @param herdada         true quando o mês não tem renda própria e vale a do mês anterior mais recente
 */
public record RendaResponse(String competencia, BigDecimal valor, String origem, boolean herdada) {
}
