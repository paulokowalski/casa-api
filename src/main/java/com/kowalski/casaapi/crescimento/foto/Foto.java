package com.kowalski.casaapi.crescimento.foto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(schema = "crescimento", name = "foto")
public class Foto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID familiaId;

    @Column(nullable = false)
    private UUID criancaId;

    @Column(nullable = false, length = 200)
    private String chave;

    @Column(nullable = false)
    private Integer largura;

    @Column(nullable = false)
    private Integer altura;

    @Column(nullable = false)
    private Long bytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    private LocalDateTime tiradaEm;

    private Long enviadaPor;

    private UUID registroId;

    private Short ordem;

    @Column(name = "criado_em", nullable = false)
    private Instant criadaEm = Instant.now();
}
