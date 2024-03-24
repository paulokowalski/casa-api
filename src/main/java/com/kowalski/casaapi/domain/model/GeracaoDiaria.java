package com.kowalski.casaapi.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "geracao_diaria")
public class GeracaoDiaria implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private float geracao;

    @Column(name = "dt_geracao")
    private LocalDate dataGeracao;

}
