package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.dto.TransacaoDTO;
import com.kowalski.casaapi.business.model.Transacao;

import java.time.LocalDate;
import java.util.List;

public interface TransacaoService {

    Transacao criar(TransacaoDTO dto);

    List<Transacao> listarPorPessoaAnoMes(Long pessoaId, Integer ano, Integer mes);

    List<Transacao> buscarTodosProximos30Dias();

    Transacao editar(Long id, TransacaoDTO dto);

    void editarSerie(String idSerie, TransacaoDTO dto, LocalDate aPartirDe);

    void excluir(Long id);

    void excluirSerie(String idSerie, LocalDate aPartirDe);
}
