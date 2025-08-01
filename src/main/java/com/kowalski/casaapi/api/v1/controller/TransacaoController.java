package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.dto.TransacaoDTO;
import com.kowalski.casaapi.business.model.Transacao;
import com.kowalski.casaapi.business.service.TransacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/transacoes")
public class TransacaoController {

    private final TransacaoService transacaoService;

    @PostMapping
    public ResponseEntity<Transacao> criar(
            @RequestBody TransacaoDTO dto
    ) {
        var transaco = transacaoService.criar(dto);
        return ResponseEntity.ok(transaco);
    }

    @GetMapping
    public List<Transacao> listarPorPessoaAnoMes(
            @RequestParam Long pessoaId,
            @RequestParam Integer ano,
            @RequestParam Integer mes
    ) {
        return transacaoService.listarPorPessoaAnoMes(pessoaId, ano, mes);
    }

    @GetMapping("/proximos-30-dias")
    public List<Transacao> buscarTodosProximos30Dias() {
        return transacaoService.buscarTodosProximos30Dias();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transacao> editar(
            @PathVariable Long id,
            @RequestBody TransacaoDTO dto
    ) {
        return ResponseEntity.ok(transacaoService.editar(id, dto));
    }

    @PutMapping("/serie/{idSerie}")
    public ResponseEntity<Void> editarSerie(
            @PathVariable String idSerie,
            @RequestBody TransacaoDTO dto,
            @RequestParam(required = false) LocalDate aPartirDe
    ) {
        transacaoService.editarSerie(idSerie, dto, aPartirDe);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id
    ) {
        transacaoService.excluir(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/serie/{idSerie}")
    public ResponseEntity<Void> excluirSerie(
            @PathVariable String idSerie,
            @RequestParam(required = false) LocalDate aPartirDe
    ) {
        transacaoService.excluirSerie(idSerie, aPartirDe);
        return ResponseEntity.ok().build();
    }
}