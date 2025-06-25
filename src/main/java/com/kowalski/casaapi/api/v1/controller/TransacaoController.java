package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.dto.TransacaoDTO;
import com.kowalski.casaapi.domain.model.Pessoa;
import com.kowalski.casaapi.domain.model.Transacao;
import com.kowalski.casaapi.domain.repository.PessoaRepository;
import com.kowalski.casaapi.domain.repository.TransacaoRepository;
import com.kowalski.casaapi.domain.service.impl.TransacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/transacoes")
public class TransacaoController {

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    // NOVO: Geração de UUID para série
    private String gerarIdSerie() {
        return UUID.randomUUID().toString();
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody TransacaoDTO dto) {
        Pessoa pessoa = pessoaRepository.findById(dto.getPessoa())
                .orElseThrow(() -> new RuntimeException("Pessoa não encontrada"));

        if (Boolean.TRUE.equals(dto.getFixa())) {
            String idSerie = gerarIdSerie();
            for (int i = 0; i < 12; i++) {
                Transacao transacao = new Transacao();
                transacao.setTipo(dto.getTipo());
                transacao.setDescricao(dto.getDescricao());
                transacao.setValor(dto.getValor());
                LocalDate dataBase = dto.getData().plusMonths(i);
                transacao.setData(dataBase);
                transacao.setFixa(true);
                transacao.setPessoa(pessoa);
                transacao.setAno(dataBase.getYear());
                transacao.setMes(dataBase.getMonthValue());
                transacao.setIdSerie(idSerie); // NOVO
                transacaoRepository.save(transacao);
            }
            return ResponseEntity.ok().build();
        } else {
            Transacao transacao = new Transacao();
            transacao.setTipo(dto.getTipo());
            transacao.setDescricao(dto.getDescricao());
            transacao.setValor(dto.getValor());
            transacao.setData(dto.getData());
            transacao.setFixa(false);
            transacao.setPessoa(pessoa);
            transacao.setAno(dto.getData().getYear());
            transacao.setMes(dto.getData().getMonthValue());
            transacaoRepository.save(transacao);
            return ResponseEntity.ok(transacao);
        }
    }

    @GetMapping
    public List<Transacao> listarPorPessoaAnoMes(
            @RequestParam Long pessoaId,
            @RequestParam Integer ano,
            @RequestParam Integer mes
    ) {
        return transacaoRepository.findByPessoaIdAndDataAnoAndDataMes(pessoaId, ano, mes);
    }

    // NOVO: Editar uma transação individual
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody TransacaoDTO dto) {
        Transacao transacao = transacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        transacao.setTipo(dto.getTipo());
        transacao.setDescricao(dto.getDescricao());
        transacao.setValor(dto.getValor());
        transacao.setData(dto.getData());
        transacao.setFixa(dto.getFixa());
        transacao.setAno(dto.getData().getYear());
        transacao.setMes(dto.getData().getMonthValue());
        transacaoRepository.save(transacao);
        return ResponseEntity.ok(transacao);
    }

    // NOVO: Editar toda a série (ou a partir de uma data)
    @PutMapping("/serie/{idSerie}")
    public ResponseEntity<?> editarSerie(
            @PathVariable String idSerie,
            @RequestBody TransacaoDTO dto,
            @RequestParam(required = false) LocalDate aPartirDe // opcional
    ) {
        List<Transacao> transacoes = transacaoRepository.findByIdSerie(idSerie);
        for (Transacao t : transacoes) {
            if (aPartirDe == null || !t.getData().isBefore(aPartirDe)) {
                t.setTipo(dto.getTipo());
                t.setDescricao(dto.getDescricao());
                t.setValor(dto.getValor());
                // Se quiser atualizar a data, pode ajustar aqui
                t.setFixa(dto.getFixa());
                transacaoRepository.save(t);
            }
        }
        return ResponseEntity.ok().build();
    }

    // NOVO: Excluir uma transação individual
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        transacaoRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // NOVO: Excluir toda a série (ou a partir de uma data)
    @DeleteMapping("/serie/{idSerie}")
    public ResponseEntity<?> excluirSerie(
            @PathVariable String idSerie,
            @RequestParam(required = false) LocalDate aPartirDe // opcional
    ) {
        List<Transacao> transacoes = transacaoRepository.findByIdSerie(idSerie);
        for (Transacao t : transacoes) {
            if (aPartirDe == null || !t.getData().isBefore(aPartirDe)) {
                transacaoRepository.delete(t);
            }
        }
        return ResponseEntity.ok().build();
    }
}