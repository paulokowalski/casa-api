package com.kowalski.casaapi.renda;

import com.kowalski.casaapi.pessoa.PessoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RendaService {

    private final RendaMensalRepository rendaRepository;
    private final PessoaService pessoaService;

    @Transactional(readOnly = true)
    public RendaResponse obter(Long pessoaId, LocalDate competencia) {
        Assert.notNull(pessoaId, "A pessoa é obrigatória");
        return rendaRepository
                .findFirstByIdPessoaIdAndIdCompetenciaLessThanEqualOrderByIdCompetenciaDesc(pessoaId, competencia)
                .map(renda -> RendaResponse.de(competencia, renda))
                .orElse(RendaResponse.semValor(competencia));
    }

    @Transactional
    public RendaResponse salvar(Long pessoaId, LocalDate competencia, RendaInput input) {
        Assert.notNull(pessoaId, "A pessoa é obrigatória");
        pessoaService.garantirQueExiste(pessoaId);
        var id = new RendaMensal.Id(pessoaId, competencia);
        if (input == null || input.removeOValorDoMes()) {
            rendaRepository.deleteById(id);
        } else {
            rendaRepository.save(new RendaMensal(id, input.valor()));
        }
        return obter(pessoaId, competencia);
    }
}
