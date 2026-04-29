package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.business.model.Carro;
import com.kowalski.casaapi.business.service.CarroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/carros", produces = MediaType.APPLICATION_JSON_VALUE)
public class CarroController {

    private final CarroService carroService;

    @GetMapping("/{codigoFipe}")
    public List<Carro> buscarPorCodigoFipe(@PathVariable String codigoFipe) {
        return carroService.buscarPorCodigoFipe(codigoFipe);
    }

}
