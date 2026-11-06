package com.kowalski.casaapi.business.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

/** Fatura do cartão de uma pessoa marcada como paga em um mês. */
@Entity
@Table(name = "fatura_paga")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FaturaPaga {

    @EmbeddedId
    private Id id;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "pessoa_id", nullable = false)
        private Long pessoaId;

        /** Mês da fatura (dia 1) */
        @Column(nullable = false)
        private LocalDate competencia;
    }
}
