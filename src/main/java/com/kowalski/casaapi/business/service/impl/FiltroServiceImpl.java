package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.response.FiltroResponse;
import com.kowalski.casaapi.business.dao.FiltroDao;
import com.kowalski.casaapi.business.service.FiltroService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FiltroServiceImpl implements FiltroService {

    private final FiltroDao filtroDao;

    @Override
    public List<FiltroResponse> buscarCategorias() {
        return filtroDao.buscarCategorias();
    }

    public List<FiltroResponse> buscarAnos() {
        return filtroDao.buscarAnos();
    }

    public List<FiltroResponse> buscarMeses(String ano) {
        return filtroDao.buscarMeses(ano);
    }

    public List<FiltroResponse> buscarPessoas(String ano, String mes) {
        return filtroDao.buscarPessoas(ano, mes);
    }

    public List<FiltroResponse> buscarCartoes(String ano, String mes, String pessoa) {
        return filtroDao.buscarCartoes(ano, mes, pessoa);
    }

}