package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.dto.GeradoDTO;
import com.kowalski.casaapi.api.v1.response.GeracaoDiariaDTO;
import com.kowalski.casaapi.business.service.GeracaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/geracao-solar", produces = MediaType.APPLICATION_JSON_VALUE)
public class GeracaoController {

    private final GeracaoService geracaoService;

    @GetMapping
    public ResponseEntity<GeracaoDiariaDTO> buscarPorData(
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        log.info("Buscando geração para data: {}", data);
        return ResponseEntity.of(Optional.ofNullable(geracaoService.buscarGeracao(data)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public void salvar(@RequestBody GeradoDTO dto) {
      geracaoService.salvar(dto);
    }

}