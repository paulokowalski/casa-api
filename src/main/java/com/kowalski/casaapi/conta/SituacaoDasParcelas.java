package com.kowalski.casaapi.conta;

import java.math.BigDecimal;

public record SituacaoDasParcelas(Integer parcelaAtual, Integer restantes, BigDecimal saldo) {

    static final SituacaoDasParcelas NAO_PARCELADA = new SituacaoDasParcelas(null, null, null);

    static SituacaoDasParcelas de(Integer parcelaAtual, int restantes, BigDecimal valorDaParcela) {
        return new SituacaoDasParcelas(parcelaAtual, restantes, valorDaParcela.multiply(BigDecimal.valueOf(restantes)));
    }
}
