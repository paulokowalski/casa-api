package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.assembler.RastreamentoAssembler;
import com.kowalski.casaapi.api.v1.dto.RastreamentoDto;
import com.kowalski.casaapi.api.v1.input.RastreamentoInput;
import com.kowalski.casaapi.domain.service.RastreamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/rastreamento", produces = MediaType.APPLICATION_JSON_VALUE)
public class RastreamentoController {

    private final RastreamentoService rastreamentoService;
    private final RastreamentoAssembler rastreamentoAssembler;

    @PostMapping
    public void salvar(@RequestBody RastreamentoInput rastreamentoInput){
        rastreamentoService.salvar(rastreamentoAssembler.to(rastreamentoInput));
    }

    @GetMapping
    public List<RastreamentoDto> listar() {
        return rastreamentoService.listar().stream().map(rastreamentoAssembler::to).toList();
    }

    @PutMapping("/{codigo}")
    public void atualizar(@PathVariable String codigo, @RequestBody RastreamentoInput rastreamentoInput) {
        log.info("Recebendo atualizaçao: {}, {}", codigo, rastreamentoInput.toString());
        rastreamentoService.atualizar(codigo, rastreamentoAssembler.to(rastreamentoInput));
    }

    @DeleteMapping("/{codigo}")
    public void deletar(@PathVariable UUID codigo) {
        rastreamentoService.deletar(codigo);
    }

    @DeleteMapping("/rastreio/{rastreio}")
    public void deletarPorRastreio(@PathVariable String rastreio) {
        rastreamentoService.deletar(rastreio);
    }

}