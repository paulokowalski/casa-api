package com.kowalski.casaapi.business.enumerator;

/** Como uma conta fora do cartão se repete ao longo dos meses. */
public enum TipoConta {
    /** Todo mês, sem data para acabar (plano de saúde, escola, internet) */
    RECORRENTE,
    /** Número fixo de parcelas (financiamento, IPTU) */
    PARCELADA,
    /** Um único mês */
    UNICA
}
