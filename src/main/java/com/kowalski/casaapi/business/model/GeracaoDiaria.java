package com.kowalski.casaapi.business.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Builder
@Getter @Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "geracao_diaria")
public class GeracaoDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Double geracao;

    @Column(name = "data")
    private LocalDate data;

}

