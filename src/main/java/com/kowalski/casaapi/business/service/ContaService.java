package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.input.ContaInput;
import com.kowalski.casaapi.api.v1.input.LancamentoInput;
import com.kowalski.casaapi.api.v1.response.ContaResponse;
import com.kowalski.casaapi.api.v1.response.LancamentoResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ContaService {

    List<ContaResponse> listar(Long pessoaId);

    ContaResponse criar(ContaInput input);

    /** Altera a conta; os meses não pagos a partir de {@code aPartirDe} passam a usar os novos dados. */
    ContaResponse editar(UUID id, ContaInput input, LocalDate aPartirDe);

    /** Sem {@code aPartirDe}: apaga a conta e o histórico. Com: encerra a conta antes desse mês. */
    void excluir(UUID id, LocalDate aPartirDe);

    /** Meses das contas da pessoa no período, gerando os que ainda não existem. */
    List<LancamentoResponse> lancamentos(Long pessoaId, LocalDate de, LocalDate ate);

    LancamentoResponse atualizarLancamento(UUID id, LancamentoInput input);
}
