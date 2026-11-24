package com.kowalski.casaapi.crescimento.exame;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(schema = "crescimento", name = "exame")
public class Exame {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID familiaId;

    @Column(nullable = false)
    private UUID criancaId;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false)
    private LocalDate data;

    @Column(length = 1000)
    private String observacao;

    @Column(nullable = false, length = 200)
    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private TipoExame tipo;

    @Column(nullable = false, length = 200)
    private String chave;

    @Column(nullable = false)
    private Long bytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    private Long enviadoPor;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();
}
