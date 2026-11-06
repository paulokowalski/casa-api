package com.kowalski.casaapi.api.v1.input;

import java.time.LocalDate;

/** @param dataPagamento padrão: hoje, quando {@code paga = true} */
public record FaturaInput(boolean paga, LocalDate dataPagamento) {
}
