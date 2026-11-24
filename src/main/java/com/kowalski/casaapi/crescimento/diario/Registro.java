package com.kowalski.casaapi.crescimento.diario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(schema = "crescimento", name = "registro")
public class Registro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID criancaId;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoRegistro tipo;

    @Column(length = 3000)
    private String texto;

    @Column(length = 80)
    private String profissional;

    private LocalDate proximaConsulta;

    @Column(length = 40)
    private String albumChave;

    @Column(length = 50)
    private String albumTitulo;

    private UUID medicaoId;

    private Long criadoPor;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(nullable = false)
    private Instant atualizadoEm = Instant.now();
}
