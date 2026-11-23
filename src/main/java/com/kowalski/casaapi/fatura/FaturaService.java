package com.kowalski.casaapi.fatura;

import com.kowalski.casaapi.pessoa.PessoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FaturaService {

    private final FaturaPagaRepository faturaRepository;
    private final PessoaService pessoaService;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public FaturaResponse obter(Long pessoaId, LocalDate competencia) {
        Assert.notNull(pessoaId, "A pessoa é obrigatória");
        return faturaRepository.findById(new FaturaPaga.Id(pessoaId, competencia))
                .map(paga -> FaturaResponse.paga(competencia, paga.getDataPagamento()))
                .orElse(FaturaResponse.naoPaga(competencia));
    }

    @Transactional
    public FaturaResponse salvar(Long pessoaId, LocalDate competencia, FaturaInput input) {
        Assert.notNull(pessoaId, "A pessoa é obrigatória");
        Assert.notNull(input, "Informe se a fatura está paga");
        pessoaService.garantirQueExiste(pessoaId);
        var id = new FaturaPaga.Id(pessoaId, competencia);
        if (input.paga()) {
            faturaRepository.save(new FaturaPaga(id, Objects.requireNonNullElse(input.dataPagamento(), LocalDate.now(relogio))));
        } else {
            faturaRepository.deleteById(id);
        }
        return obter(pessoaId, competencia);
    }
}
