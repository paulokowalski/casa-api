package com.kowalski.casaapi.crescimento.familia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(schema = "crescimento", name = "familia")
public class Familia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();
}
