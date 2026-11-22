package com.kowalski.casaapi.crescimento.marco;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(schema = "crescimento", name = "marco_atingido")
public class MarcoAtingido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID criancaId;

    /** Id do {@link CatalogoMarcos}. */
    @Column(nullable = false, length = 20)
    private String marcoId;

    @Column(nullable = false)
    private LocalDate atingidoEm;

    @Column(length = 500)
    private String nota;

    private Long marcadoPor;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(nullable = false)
    private Instant atualizadoEm = Instant.now();
}
