package com.kowalski.casaapi.crescimento.vacina;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(schema = "crescimento", name = "vacina_aplicada")
public class VacinaAplicada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID criancaId;

    @Column(nullable = false, length = 40)
    private String vacinaId;

    @Column(length = 80)
    private String nome;

    @Column(nullable = false)
    private LocalDate aplicadaEm;

    @Column(length = 40)
    private String lote;

    @Column(length = 80)
    private String local;

    private Long registradoPor;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(nullable = false)
    private Instant atualizadoEm = Instant.now();
}
