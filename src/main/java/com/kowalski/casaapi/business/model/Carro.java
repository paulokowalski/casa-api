package com.kowalski.casaapi.business.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter @Setter
@Table(schema = "monitoramento")
@Schema(description = "Valor FIPE de um veículo num mês")
public class Carro {

    @Schema(description = "Id")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Schema(description = "Código FIPE", example = "015203-0")
    private String codigoVeiculo;

    @Schema(description = "Mês de referência da tabela FIPE")
    private String mesReferencia;

    @Schema(description = "Marca")
    private String marca;

    @Schema(description = "Modelo")
    private String modelo;

    @Schema(description = "Ano/modelo")
    private String ano;

    @Schema(description = "Valor na tabela (R$)")
    private Double valor;

}
