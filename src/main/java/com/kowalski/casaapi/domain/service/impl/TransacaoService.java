package com.kowalski.casaapi.domain.service.impl;

import com.kowalski.casaapi.domain.model.Transacao;
import com.kowalski.casaapi.domain.repository.TransacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TransacaoService {

    @Autowired
    private TransacaoRepository transacaoRepository;

}