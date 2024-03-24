package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.assembler.GeracaoAssembler;
import com.kowalski.casaapi.api.v1.dto.GeracaoDto;
import com.kowalski.casaapi.domain.service.GeracaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/geracao", produces = MediaType.APPLICATION_JSON_VALUE)
public class GeracaoController {

    private final GeracaoService geracaoService;
    private final GeracaoAssembler geracaoAssembler;

    @GetMapping
    public GeracaoDto buscarGeracaoAtual(){
        return geracaoAssembler.to(geracaoService.buscarGeracaoAtual());
    }

    @GetMapping("/mes/{mes}")
    public List<GeracaoDto> buscarListaGeracaoMes(@PathVariable String mes){
       return geracaoService.buscarListaGeracaoMes(mes).stream().map(geracaoAssembler::to).toList();
    }

    @GetMapping("/mestotal/{mes}")
    public GeracaoDto buscarGeracaoMes(@PathVariable String mes){
        var geracao = geracaoService.buscarGeracaoMes(mes);
        return Objects.isNull(geracao) ? new GeracaoDto(0, null) : geracaoAssembler.to(geracao);
    }

}