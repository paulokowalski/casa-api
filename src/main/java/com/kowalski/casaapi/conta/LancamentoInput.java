package com.kowalski.casaapi.conta;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Alterações num lançamento; campos nulos não mudam")
public record LancamentoInput(
        @Schema(description = "Marca como pago ou não pago", example = "true") Boolean paga,
        @Schema(description = "Valor real do mês (maior que zero)", example = "187.45") BigDecimal valor,
        @Schema(description = "Data do pagamento (aaaa-mm-dd); padrão: hoje", example = "2026-11-10") LocalDate dataPagamento) {
}
