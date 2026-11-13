package com.kowalski.casaapi.api.v1.response;

import java.time.LocalDate;

public record FaturaResponse(String competencia, boolean paga, LocalDate dataPagamento) {
}
