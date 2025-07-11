package com.kowalski.casaapi.business.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Builder
@Getter @Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "geracao_mensal")
public class GeracaoMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Double geracao;

    private String mes;

}

