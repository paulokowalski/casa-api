package com.kowalski.casaapi.business.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

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

        @Column(nullable = false)
        private LocalDate competencia;
    }
}
