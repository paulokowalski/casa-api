package com.kowalski.casaapi.api.v1.input;

import java.time.LocalDate;

public record FaturaInput(boolean paga, LocalDate dataPagamento) {
}
