package com.kowalski.casaapi.domain.service;

import com.kowalski.casaapi.domain.model.Cartao;
import org.springframework.stereotype.Service;

@Service
public interface CartaoService {

    Cartao findByNome(String nome);

}
