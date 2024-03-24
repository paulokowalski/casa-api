package com.kowalski.casaapi.domain.service.impl;

import com.kowalski.casaapi.domain.model.Geracao;
import com.kowalski.casaapi.domain.repository.GeracaoRepository;
import com.kowalski.casaapi.domain.service.GeracaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeracaoServiceImpl implements GeracaoService {

    private final GeracaoRepository geracaoRepository;

    public Geracao buscarGeracaoAtual() {
        return geracaoRepository.findFirstByOrderByDataCadastroDesc();
    }

    public List<Geracao> buscarListaGeracaoMes(String mes){
        return geracaoRepository.buscarGeracaoPorMes(mes).stream()
                .collect(Collectors.groupingBy(
                        r -> r.getDataCadastro().toLocalDate(),
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(Geracao::getDataCadastro)),
                                opt -> opt.orElse(null)
                        )
                ))
                .values().stream().toList();
    }

    public Geracao buscarGeracaoMes(String mes){
        var geracoesMes = geracaoRepository.buscarGeracaoPorMes(mes).stream()
                .collect(Collectors.groupingBy(
                        r -> r.getDataCadastro().toLocalDate(),
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(Geracao::getDataCadastro)),
                                opt -> opt.orElse(null)
                        )
                ))
                .values().stream().toList();
        if(!geracoesMes.isEmpty()){
            var primeiraGeracao = geracoesMes.stream().filter(ge -> ge.getDataCadastro().getDayOfMonth() == 1).findAny();
            return Geracao
                    .builder()
                    .dataCadastro(primeiraGeracao.get().getDataCadastro())
                    .geracao((float)geracoesMes.stream().mapToDouble(Geracao::getGeracao).sum())
                    .build();
        }
        return null;
    }

    @Override
    public Geracao buscarGeracaoPorDiaMes(String dia, String mes, String ano) {
        return geracaoRepository.buscarGeracaoPorDiaMesAno(dia, mes, ano);
    }

}
