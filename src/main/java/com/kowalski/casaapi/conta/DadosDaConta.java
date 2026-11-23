package com.kowalski.casaapi.conta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosDaConta(String descricao, TipoConta tipo, BigDecimal valor, Integer diaVencimento, LocalDate inicio,
                           LocalDate fim, Integer parcelaInicial, Integer totalParcelas, BigDecimal jurosMes,
                           boolean valorVariavel, boolean debitoAutomatico) {
}
