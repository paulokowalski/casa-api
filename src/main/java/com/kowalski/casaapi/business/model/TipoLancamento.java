package com.kowalski.casaapi.business.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@Table
@AllArgsConstructor
@RequiredArgsConstructor
public class TipoLancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;

}
