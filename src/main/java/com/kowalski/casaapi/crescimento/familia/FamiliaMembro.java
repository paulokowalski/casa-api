package com.kowalski.casaapi.crescimento.familia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(schema = "crescimento", name = "familia_membro")
public class FamiliaMembro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID familiaId;

    /** Id do usuário no sistema de login. */
    @Column(nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PapelFamilia papel;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();
}
