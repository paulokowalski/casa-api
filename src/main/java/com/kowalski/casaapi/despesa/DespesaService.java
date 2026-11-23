package com.kowalski.casaapi.despesa;

import com.kowalski.casaapi.comum.Competencias;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;

    @Transactional(readOnly = true)
    public DespesaAnualResponse buscarDespesasAnuais(String ano, String pessoa) {
        Assert.hasText(ano, "O ano não pode estar vazio");
        Assert.hasText(pessoa, "O nome da pessoa não pode estar vazio");
        String nome = pessoa.toUpperCase();

        Map<String, DespesaResponse> despesasPorMes = new TreeMap<>();
        IntStream.rangeClosed(1, 12).mapToObj("%02d"::formatted).forEach(mes -> {
            DespesaResponse despesa = despesaRepository.totaisDoMes(nome, Competencias.de(ano, mes));
            if (despesa != null) despesasPorMes.put(mes, despesa);
        });
        return new DespesaAnualResponse(nome, ano, despesasPorMes);
    }
}
