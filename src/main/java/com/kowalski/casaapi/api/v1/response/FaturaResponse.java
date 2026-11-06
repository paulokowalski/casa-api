package com.kowalski.casaapi.api.v1.response;

import java.time.LocalDate;

/** Situação da fatura do cartão no mês ({@code competencia} no formato {@code yyyy-MM}). */
public record FaturaResponse(String competencia, boolean paga, LocalDate dataPagamento) {
}
