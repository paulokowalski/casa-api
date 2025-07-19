package com.kowalski.casaapi.business.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.UUID;

@Entity
@Builder
@Getter @Setter
@Table(name = "lista_compra")
@AllArgsConstructor
@RequiredArgsConstructor
public class ListaCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotEmpty
    private String item;

}
