package com.kowalski.casaapi.crescimento.ia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Resumo de um mês de vida gerado pela IA (mes = 0 é o primeiro mês). */
@Entity
@Getter @Setter
@Table(schema = "crescimento", name = "resumo_mes")
public class ResumoMes {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID criancaId;

    @Column(nullable = false)
    private int mes;

    @Column(nullable = false)
    private LocalDate inicio;

    @Column(nullable = false)
    private LocalDate fim;

    @Column(nullable = false, length = 4000)
    private String texto;

    /** Uma pergunta por linha. */
    @Column(nullable = false, length = 4000)
    private String perguntas = "";

    private String modelo;

    private Long geradoPor;

    @Column(nullable = false)
    private Instant geradoEm = Instant.now();
}
