package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.business.model.Carro;
import com.kowalski.casaapi.business.repository.CarroRepository;
import com.kowalski.casaapi.business.service.CarroService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarroServiceImpl implements CarroService {

    private final CarroRepository carroRepository;

    @Override
    public List<Carro> buscarPorCodigoFipe(String codigoFipe) {
        return carroRepository.findByCodigoVeiculoOrderByMesReferencia(codigoFipe);
    }

}
