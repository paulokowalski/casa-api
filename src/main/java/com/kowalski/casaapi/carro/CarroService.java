package com.kowalski.casaapi.carro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarroService {

    private final CarroRepository carroRepository;

    @Transactional(readOnly = true)
    public List<CarroResponse> historico(String codigoFipe) {
        return carroRepository.historicoDoVeiculo(codigoFipe);
    }
}
