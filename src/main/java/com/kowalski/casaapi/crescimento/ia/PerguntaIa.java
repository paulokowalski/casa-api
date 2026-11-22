package com.kowalski.casaapi.crescimento.ia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Pergunta dos pais à IA sobre um mês de vida, com a resposta. */
@Entity
@Getter @Setter
@Table(schema = "crescimento", name = "pergunta_ia")
public class PerguntaIa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID criancaId;

    @Column(nullable = false)
    private int mes;

    @Column(nullable = false, length = 500)
    private String pergunta;

    @Column(nullable = false, length = 3000)
    private String resposta;

    private String modelo;

    private Long feitaPor;

    @Column(nullable = false)
    private Instant feitaEm = Instant.now();
}
