package com.kowalski.casaapi.business.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter @Setter
@Builder
public class Geracao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Double geracao;

    private Double potencia;

}
