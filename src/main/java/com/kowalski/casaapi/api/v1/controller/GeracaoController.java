package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.business.service.GeracaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/geracao-solar", produces = MediaType.APPLICATION_JSON_VALUE)
public class GeracaoController {

    private final GeracaoService geracaoService;

    @PostMapping
    public void salvar(@Param("geracao") Double geracao){
      geracaoService.salvar(geracao);
    }

}
