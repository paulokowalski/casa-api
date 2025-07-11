package com.kowalski.casaapi.business.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@Table(schema = "financa")
@AllArgsConstructor
@RequiredArgsConstructor
public class Cartao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;

}
