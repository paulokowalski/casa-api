package com.kowalski.casaapi.carro;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter @Setter
@Table(schema = "monitoramento")
public class Carro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String codigoVeiculo;

    private String mesReferencia;

    private String marca;

    private String modelo;

    private String ano;

    private Double valor;
}
