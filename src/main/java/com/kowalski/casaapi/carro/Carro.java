package com.kowalski.casaapi.carro;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Column(insertable = false, updatable = false)
    private LocalDate mes;

    private String marca;

    private String modelo;

    private String ano;

    private BigDecimal valor;
}
