package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.dto.GeradoDTO;
import com.kowalski.casaapi.api.v1.response.GeracaoDiariaResponse;
import com.kowalski.casaapi.api.v1.response.GeracaoMensalResponse;
import com.kowalski.casaapi.api.v1.response.GeracaoMesAnoResponse;
import com.kowalski.casaapi.business.exception.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.service.GeracaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/geracao-solar", produces = MediaType.APPLICATION_JSON_VALUE)
public class GeracaoController {

    private final GeracaoService geracaoService;

    private static <T> T encontrado(T valor) {
        if (valor == null) throw new RecursoNaoEncontradoException("Sem dados de geração");
        return valor;
    }

    @GetMapping
    public GeracaoDiariaResponse doDia(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return encontrado(geracaoService.buscarGeracao(data));
    }

    @GetMapping("/{ano}/{mes}")
    public GeracaoMensalResponse doMes(@PathVariable int ano, @PathVariable int mes) {
        return encontrado(geracaoService.buscarGeracaoMensal(ano, mes));
    }

    @GetMapping("/{ano}")
    public List<GeracaoMesAnoResponse> doAno(@PathVariable int ano) {
        return encontrado(geracaoService.buscarGeracaoAnual(ano));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public void salvar(@RequestBody GeradoDTO dto) {
        geracaoService.salvar(dto);
    }
}
