package com.kowalski.casaapi.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class Rastreamento implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String codigo;

    private String nome;

    private String status;

    private boolean finalizado;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "rastreamento", cascade = CascadeType.ALL)
    private List<RastreamentoDetalhe> detalhes;

}