package com.kowalski.casaapi.crescimento.medida;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(schema = "crescimento", name = "medicao")
public class Medicao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID criancaId;

    @Column(nullable = false)
    private LocalDate data;

    private Double pesoKg;

    private Double comprimentoCm;

    private Double perimetroCm;

    @Column(length = 1000)
    private String observacao;

    private Long criadoPor;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(nullable = false)
    private Instant atualizadoEm = Instant.now();
}
