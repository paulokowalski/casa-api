package com.kowalski.casaapi.filtro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FiltroService {

    private final FiltroRepository filtroRepository;

    public List<FiltroResponse> buscarCategorias() {
        return filtroRepository.categorias();
    }

    public List<FiltroResponse> buscarAnos() {
        return filtroRepository.anos();
    }

    public List<FiltroResponse> buscarMeses(String ano) {
        return filtroRepository.meses(ano);
    }

    public List<FiltroResponse> buscarPessoas(String ano, String mes) {
        return filtroRepository.pessoas(ano, mes);
    }

    public List<FiltroResponse> buscarCartoes(String ano, String mes, String pessoa) {
        return filtroRepository.cartoes(ano, mes, pessoa);
    }

    public List<FiltroResponse> buscarTiposDeLancamentos() {
        return filtroRepository.tiposDeLancamento();
    }
}
