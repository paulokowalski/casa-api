package com.kowalski.casaapi.renda;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Renda do mês")
public record RendaInput(
        @Schema(description = "Valor da renda; vazio ou zero remove o valor do mês", example = "8500.00") BigDecimal valor) {

    public boolean removeOValorDoMes() {
        return valor == null || valor.signum() <= 0;
    }
}
