package com.kowalski.casaapi.api.v1.input;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoInput(Boolean paga, BigDecimal valor, LocalDate dataPagamento) {
}
