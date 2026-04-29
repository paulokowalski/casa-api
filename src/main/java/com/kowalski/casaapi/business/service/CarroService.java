package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.business.model.Carro;

import java.util.List;

public interface CarroService {

    List<Carro> buscarPorCodigoFipe(String codigoFipe);

}
