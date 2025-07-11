package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.dto.GeradoDTO;
import com.kowalski.casaapi.business.service.GeracaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/geracao-solar")
public class GeracaoController {

    private final GeracaoService geracaoService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public void salvar(@RequestBody GeradoDTO dto) {
      geracaoService.salvar(dto);
    }

}
