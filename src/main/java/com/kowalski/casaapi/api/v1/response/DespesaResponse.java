package com.kowalski.casaapi.api.v1.response;

import java.math.BigDecimal;

public record DespesaResponse (
    BigDecimal valorMes,
    BigDecimal valorProximoMes,
    BigDecimal valorSaindo,
    BigDecimal valorParcelaSaindo,
    BigDecimal valorSaindoTotal
)
{ }