package com.kowalski.casaapi.api.v1.response;

import java.math.BigDecimal;

public record RendaResponse(String competencia, BigDecimal valor, String origem, boolean herdada) {
}
