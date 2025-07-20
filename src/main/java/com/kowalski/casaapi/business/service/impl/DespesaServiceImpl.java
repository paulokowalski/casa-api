package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.response.DespesaResponse;
import com.kowalski.casaapi.api.v1.response.DespesaAnualResponse;
import com.kowalski.casaapi.business.dao.DespesaDao;
import com.kowalski.casaapi.business.service.DespesaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class DespesaServiceImpl implements DespesaService {

    private final DespesaDao despesaDao;
    private static final String[] MESES = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};

    @Override
    public DespesaResponse buscarPorAnoMesNome(String ano, String mes, String pessoa) {
        Assert.hasText(ano, "O ano não pode estar vazio");
        Assert.hasText(mes, "O mês não pode estar vazio");
        Assert.hasText(pessoa, "O nome da pessoa não pode estar vazio");

        return despesaDao.findByNomePessoaAndMesAnoReferencia(
            pessoa.toUpperCase(), 
            mes, 
            ano
        );
    }

    @Override
    public DespesaAnualResponse buscarDespesasAnuais(String ano, String pessoa) {
        Assert.hasText(ano, "O ano não pode estar vazio");
        Assert.hasText(pessoa, "O nome da pessoa não pode estar vazio");
        
        Map<String, DespesaResponse> despesasPorMes = new TreeMap<>();
        String pessoaUpperCase = pessoa.toUpperCase();
        
        Arrays.stream(MESES).forEach(mes -> {
            DespesaResponse despesa = buscarPorAnoMesNome(ano, mes, pessoaUpperCase);
            if (despesa != null) {
                despesasPorMes.put(mes, despesa);
            }
        });

        return new DespesaAnualResponse(
            pessoaUpperCase,
            ano,
            despesasPorMes
        );
    }
}