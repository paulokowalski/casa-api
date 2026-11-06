package com.kowalski.casaapi.api.v1.input;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alteração parcial de um mês de uma conta. Campos nulos não mudam.
 *
 * @param valor          valor real do mês (confirma a estimativa de contas de valor variável)
 * @param dataPagamento  padrão: hoje, quando {@code paga = true}
 */
public record LancamentoInput(Boolean paga, BigDecimal valor, LocalDate dataPagamento) {
}
