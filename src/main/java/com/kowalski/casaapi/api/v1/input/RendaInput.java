package com.kowalski.casaapi.api.v1.input;

import java.math.BigDecimal;

/** Renda líquida do mês. {@code null} ou zero remove a renda informada para o mês. */
public record RendaInput(BigDecimal valor) {
}
