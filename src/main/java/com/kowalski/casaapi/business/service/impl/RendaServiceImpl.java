package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.input.RendaInput;
import com.kowalski.casaapi.api.v1.response.RendaResponse;
import com.kowalski.casaapi.business.exception.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.model.RendaMensal;
import com.kowalski.casaapi.business.repository.PessoaRepository;
import com.kowalski.casaapi.business.repository.RendaMensalRepository;
import com.kowalski.casaapi.business.service.RendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDate;

import static com.kowalski.casaapi.util.Competencias.formatar;

@Service
@RequiredArgsConstructor
public class RendaServiceImpl implements RendaService {

    private final RendaMensalRepository rendaRepository;
    private final PessoaRepository pessoaRepository;

    @Override
    @Transactional(readOnly = true)
    public RendaResponse obter(Long pessoaId, LocalDate competencia) {
        Assert.notNull(pessoaId, "A pessoa é obrigatória");
        return rendaRepository
                .findFirstByIdPessoaIdAndIdCompetenciaLessThanEqualOrderByIdCompetenciaDesc(pessoaId, competencia)
                .map(r -> new RendaResponse(formatar(competencia), r.getValor(), formatar(r.getId().getCompetencia()),
                        !r.getId().getCompetencia().equals(competencia)))
                .orElse(new RendaResponse(formatar(competencia), null, null, false));
    }

    @Override
    @Transactional
    public RendaResponse salvar(Long pessoaId, LocalDate competencia, RendaInput input) {
        Assert.notNull(pessoaId, "A pessoa é obrigatória");
        if (!pessoaRepository.existsById(pessoaId)) {
            throw new RecursoNaoEncontradoException("Pessoa " + pessoaId + " não encontrada");
        }
        var id = new RendaMensal.Id(pessoaId, competencia);
        if (input == null || input.valor() == null || input.valor().signum() <= 0) {
            rendaRepository.deleteById(id);
        } else {
            rendaRepository.save(new RendaMensal(id, input.valor()));
        }
        return obter(pessoaId, competencia);
    }
}
