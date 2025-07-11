package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.business.model.Cartao;
import org.springframework.stereotype.Service;

@Service
public interface CartaoService {

    Cartao findByNome(String nome);

}
