package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.dto.GeradoDTO;
import com.kowalski.casaapi.api.v1.response.GeracaoMesAnoResponse;
import com.kowalski.casaapi.api.v1.response.GeracaoDiariaResponse;
import com.kowalski.casaapi.api.v1.response.GeracaoMensalResponse;
import com.kowalski.casaapi.business.service.GeracaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/geracao-solar", produces = MediaType.APPLICATION_JSON_VALUE)
public class GeracaoController {

    private final GeracaoService geracaoService;

    @GetMapping
    public ResponseEntity<GeracaoDiariaResponse> buscarPorData(
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        log.info("Buscando geração para data: {}", data);
        return ResponseEntity.of(Optional.ofNullable(geracaoService.buscarGeracao(data)));
    }

    @GetMapping("/ano/{ano}/mes/{mes}")
    public ResponseEntity<GeracaoMensalResponse> buscarGeracaoMensalPorData(
            @PathVariable("ano") int ano,
            @PathVariable("mes") int mes
    ) {
        return ResponseEntity.of(Optional.ofNullable(geracaoService.buscarGeracaoMensal(ano, mes)));
    }

    @GetMapping("/ano/{ano}")
    public ResponseEntity<List<GeracaoMesAnoResponse>> buscarGeracaoAnualPorData(
            @PathVariable("ano") int ano
            ) {
        return ResponseEntity.of(Optional.ofNullable(geracaoService.buscarGeracaoAnual(ano)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public void salvar(@RequestBody GeradoDTO dto) {
        log.info("Recebendo dados... {}", dto.toString());
        geracaoService.salvar(dto);
    }

}