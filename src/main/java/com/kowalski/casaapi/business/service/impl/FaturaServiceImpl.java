package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.input.FaturaInput;
import com.kowalski.casaapi.api.v1.response.FaturaResponse;
import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.model.FaturaPaga;
import com.kowalski.casaapi.business.repository.FaturaPagaRepository;
import com.kowalski.casaapi.pessoa.PessoaRepository;
import com.kowalski.casaapi.business.service.FaturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import static com.kowalski.casaapi.comum.Competencias.formatar;

@Service
@RequiredArgsConstructor
public class FaturaServiceImpl implements FaturaService {

    private final FaturaPagaRepository faturaRepository;
    private final PessoaRepository pessoaRepository;
    private final Clock relogio;

    @Override
    @Transactional(readOnly = true)
    public FaturaResponse obter(Long pessoaId, LocalDate competencia) {
        Assert.notNull(pessoaId, "A pessoa é obrigatória");
        return faturaRepository.findById(new FaturaPaga.Id(pessoaId, competencia))
                .map(f -> new FaturaResponse(formatar(competencia), true, f.getDataPagamento()))
                .orElse(new FaturaResponse(formatar(competencia), false, null));
    }

    @Override
    @Transactional
    public FaturaResponse salvar(Long pessoaId, LocalDate competencia, FaturaInput input) {
        Assert.notNull(pessoaId, "A pessoa é obrigatória");
        Assert.notNull(input, "Informe se a fatura está paga");
        if (!pessoaRepository.existsById(pessoaId)) {
            throw new RecursoNaoEncontradoException("Pessoa " + pessoaId + " não encontrada");
        }
        var id = new FaturaPaga.Id(pessoaId, competencia);
        if (input.paga()) {
            faturaRepository.save(new FaturaPaga(id, Objects.requireNonNullElse(input.dataPagamento(), LocalDate.now(relogio))));
        } else {
            faturaRepository.deleteById(id);
        }
        return obter(pessoaId, competencia);
    }
}
