package com.kowalski.casaapi.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class Geracao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private float geracao;

    @Column(name = "dt_cadastro")
    private LocalDateTime dataCadastro;
}
