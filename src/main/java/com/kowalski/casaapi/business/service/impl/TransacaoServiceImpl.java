package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.dto.TransacaoDTO;
import com.kowalski.casaapi.business.model.Pessoa;
import com.kowalski.casaapi.business.model.Transacao;
import com.kowalski.casaapi.business.repository.PessoaRepository;
import com.kowalski.casaapi.business.repository.TransacaoRepository;
import com.kowalski.casaapi.business.service.TransacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransacaoServiceImpl implements TransacaoService {

    private final PessoaRepository pessoaRepository;
    private final TransacaoRepository transacaoRepository;

    @Override
    public Transacao criar(TransacaoDTO dto) {
        Pessoa pessoa = pessoaRepository.findById(dto.getPessoa())
                .orElseThrow(() -> new RuntimeException("Pessoa não encontrada"));

        if (Boolean.TRUE.equals(dto.getFixa())) {
            String idSerie = UUID.randomUUID().toString();
            for (int i = 0; i < 12; i++) {
                Transacao transacao = new Transacao();
                transacao.setTipo(dto.getTipo());
                transacao.setDescricao(dto.getDescricao());
                transacao.setValor(dto.getValor());
                LocalDate dataBase = dto.getData().plusMonths(i);
                transacao.setData(dataBase);
                transacao.setFixa(true);
                transacao.setPessoa(pessoa);
                transacao.setAno(dataBase.getYear());
                transacao.setMes(dataBase.getMonthValue());
                transacao.setIdSerie(idSerie); // NOVO
                transacaoRepository.save(transacao);
            }
            return new Transacao();
        } else {
            Transacao transacao = new Transacao();
            transacao.setTipo(dto.getTipo());
            transacao.setDescricao(dto.getDescricao());
            transacao.setValor(dto.getValor());
            transacao.setData(dto.getData());
            transacao.setFixa(false);
            transacao.setPessoa(pessoa);
            transacao.setAno(dto.getData().getYear());
            transacao.setMes(dto.getData().getMonthValue());
            transacaoRepository.save(transacao);
            return transacao;
        }
    }

    @Override
    public List<Transacao> listarPorPessoaAnoMes(Long pessoaId, Integer ano, Integer mes) {
        return transacaoRepository.findByPessoaIdAndDataAnoAndDataMes(pessoaId, ano, mes);
    }

    @Override
    public List<Transacao> buscarTodosProximos30Dias() {
        return transacaoRepository.findByData(LocalDate.now(), LocalDate.now().plusDays(30));
    }

    @Override
    public Transacao editar(Long id, TransacaoDTO dto) {
        Transacao transacao = transacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        transacao.setTipo(dto.getTipo());
        transacao.setDescricao(dto.getDescricao());
        transacao.setValor(dto.getValor());
        transacao.setData(dto.getData());
        transacao.setFixa(dto.getFixa());
        transacao.setAno(dto.getData().getYear());
        transacao.setMes(dto.getData().getMonthValue());
        transacao.setPaga(dto.getPaga());
        transacaoRepository.save(transacao);
        return transacao;
    }

    @Override
    public void editarSerie(String idSerie, TransacaoDTO dto, LocalDate aPartirDe) {
        List<Transacao> transacoes = transacaoRepository.findByIdSerie(idSerie);
        for (Transacao t : transacoes) {
            if (aPartirDe == null || !t.getData().isBefore(aPartirDe)) {
                t.setTipo(dto.getTipo());
                t.setDescricao(dto.getDescricao());
                t.setValor(dto.getValor());
                t.setFixa(dto.getFixa());
                transacaoRepository.save(t);
            }
        }
    }

    @Override
    public void excluir(Long id) {
        transacaoRepository.deleteById(id);
    }

    @Override
    public void excluirSerie(String idSerie, LocalDate aPartirDe) {
        List<Transacao> transacoes = transacaoRepository.findByIdSerie(idSerie);
        for (Transacao t : transacoes) {
            if (aPartirDe == null || !t.getData().isBefore(aPartirDe)) {
                transacaoRepository.delete(t);
            }
        }
    }
}
