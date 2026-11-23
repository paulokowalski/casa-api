package com.kowalski.casaapi.carro;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Valor FIPE de um veículo num mês")
public record CarroResponse(
        @Schema(description = "Id") UUID id,
        @Schema(description = "Código FIPE", example = "015203-0") String codigoVeiculo,
        @Schema(description = "Mês de referência da tabela FIPE") String mesReferencia,
        @Schema(description = "Marca") String marca,
        @Schema(description = "Modelo") String modelo,
        @Schema(description = "Ano/modelo") String ano,
        @Schema(description = "Valor na tabela (R$)") Double valor) {
}
