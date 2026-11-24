package com.kowalski.casaapi.crescimento.crianca;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(schema = "crescimento", name = "crianca")
public class Crianca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID familiaId;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false)
    private LocalDate nascimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Sexo sexo;

    private Double idadeGestacionalSemanas;

    private Double pesoNascerKg;

    private Double comprimentoNascerCm;

    private Double perimetroNascerCm;

    private UUID fotoId;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(nullable = false)
    private Instant atualizadoEm = Instant.now();
}
