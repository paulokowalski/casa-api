package com.kowalski.casaapi.business.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@Getter @Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class Geracao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Double geracao;

    private Double potencia;

    @Column(name = "data")
    private LocalDateTime data;

}

