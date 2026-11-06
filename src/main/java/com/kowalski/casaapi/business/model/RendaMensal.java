package com.kowalski.casaapi.business.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Renda líquida de uma pessoa em um mês. */
@Entity
@Table(name = "renda_mensal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RendaMensal {

    @EmbeddedId
    private Id id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "pessoa_id", nullable = false)
        private Long pessoaId;

        /** Mês de referência (dia 1) */
        @Column(nullable = false)
        private LocalDate competencia;
    }
}
