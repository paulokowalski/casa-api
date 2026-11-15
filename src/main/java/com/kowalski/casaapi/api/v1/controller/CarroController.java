package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.business.model.Carro;
import com.kowalski.casaapi.business.service.CarroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Carros", description = "Histórico de valores da tabela FIPE")
public class CarroController {

    private final CarroService carroService;

    @GetMapping("/{codigoFipe}")
    @Operation(summary = "Histórico FIPE de um veículo",
            description = "Valores registrados mês a mês para o código FIPE, em ordem de mês de referência.")
    public List<Carro> buscarPorCodigoFipe(
            @Parameter(description = "Código FIPE do veículo", example = "015203-0") @PathVariable String codigoFipe) {
        return carroService.buscarPorCodigoFipe(codigoFipe);
    }
}
