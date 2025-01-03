package com.kowalski.casaapi.domain.service.impl;

import com.kowalski.casaapi.domain.model.Rastreamento;
import com.kowalski.casaapi.domain.model.RastreamentoDetalhe;
import com.kowalski.casaapi.domain.repository.RastreamentoDetalheRepository;
import com.kowalski.casaapi.domain.repository.RastreamentoRepository;
import com.kowalski.casaapi.domain.service.RastreamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RastreamentoServiceImpl implements RastreamentoService {


    private final RastreamentoRepository rastreamentoRepository;
    private final RastreamentoDetalheRepository rastreamentoDetalheRepository;

    public List<Rastreamento> listar () {
        return rastreamentoRepository.findAllByFinalizado(Boolean.FALSE);
    }

    @Override
    @Transactional
    public void salvar(Rastreamento rastreamento) {
        var rastreamentoDetalhe = RastreamentoDetalhe
                .builder()
                .rastreamento(rastreamento)
                .status(rastreamento.getStatus())
                .build();
        rastreamento.setDetalhes(Collections.singletonList(rastreamentoDetalhe));
        rastreamentoRepository.save(rastreamento);
    }

    @Override
    public void atualizar(String codigo, Rastreamento rastreamento) {
        var rastreamentoOpt = rastreamentoRepository.findByCodigo(codigo);
        if(rastreamentoOpt.isPresent()) {
            var rastreamentoDB = rastreamentoOpt.get();
            rastreamentoDB.setStatus(rastreamento.getStatus());
            rastreamentoRepository.save(rastreamentoDB);
            var rastreamentoDetalhe = RastreamentoDetalhe
                    .builder()
                    .rastreamento(rastreamentoDB)
                    .status(rastreamento.getStatus())
                    .build();
            rastreamentoDetalheRepository.save(rastreamentoDetalhe);
        }
    }

    @Override
    public void deletar(UUID codigo) {
        var rastreamentoOpt = rastreamentoRepository.findById(codigo);
        rastreamentoOpt.ifPresent(rastreamento -> {
            rastreamento.setFinalizado(Boolean.TRUE);
            rastreamentoRepository.save(rastreamento);
        });
    }

    @Override
    public void deletar(String rastreio) {
        var rastreamentoOpt = rastreamentoRepository.findByCodigo(rastreio);
        rastreamentoOpt.ifPresent(rastreamento -> {
            rastreamento.setFinalizado(Boolean.TRUE);
            rastreamentoRepository.save(rastreamento);
        });
    }
}
