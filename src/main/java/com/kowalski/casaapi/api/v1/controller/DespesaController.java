package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.response.DespesaResponse;
import com.kowalski.casaapi.api.v1.response.DespesaAnualResponse;
import com.kowalski.casaapi.domain.service.DespesaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/despesa", produces = MediaType.APPLICATION_JSON_VALUE)
public class DespesaController {

    private final DespesaService despesaService;

    @GetMapping("/{ano}/{mes}/{pessoa}")
    public ResponseEntity<DespesaResponse> buscarPorAnoMesNome(
            @PathVariable String ano,
            @PathVariable String mes,
            @PathVariable String pessoa) {
        DespesaResponse response = despesaService.buscarPorAnoMesNome(ano, mes, pessoa);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{ano}/{pessoa}")
    public ResponseEntity<DespesaAnualResponse> buscarDespesasAnuais(
            @PathVariable String ano,
            @PathVariable String pessoa) {
        DespesaAnualResponse response = despesaService.buscarDespesasAnuais(ano, pessoa);
        return response != null && !response.despesasPorMes().isEmpty() 
            ? ResponseEntity.ok(response) 
            : ResponseEntity.notFound().build();
    }
}