package com.kowalski.casaapi.domain.service;

import com.kowalski.casaapi.api.v1.response.DespesaResponse;
import com.kowalski.casaapi.domain.dao.DespesaDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaDao despesaDao;

    public DespesaResponse buscarPorAnoMesNome(String ano, String mes, String pessoa){
        return despesaDao.findByNomePessoaAndMesAnoReferencia(pessoa.toUpperCase(), mes, ano);
    }
}