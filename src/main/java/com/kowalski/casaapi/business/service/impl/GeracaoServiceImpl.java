package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.dto.GeradoDTO;
import com.kowalski.casaapi.api.v1.response.GeracaoDTO;
import com.kowalski.casaapi.api.v1.response.GeracaoDiariaDTO;
import com.kowalski.casaapi.business.model.Geracao;
import com.kowalski.casaapi.business.model.GeracaoDiaria;
import com.kowalski.casaapi.business.repository.GeracaoRepository;
import com.kowalski.casaapi.business.service.GeracaoDiariaService;
import com.kowalski.casaapi.business.service.GeracaoMensalService;
import com.kowalski.casaapi.business.service.GeracaoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GeracaoServiceImpl implements GeracaoService {

    private final GeracaoRepository geracaoRepository;
    private final GeracaoDiariaService geracaoDiariaService;
    private final GeracaoMensalService geracaoMensalService;

    @Override
    public GeracaoDiariaDTO buscarGeracao(LocalDate data) {
        var geracoes = geracaoRepository.buscarPorData(data).stream().map(ger -> new GeracaoDTO(ger.getPotencia(), ger.getData())).toList();
        var gerado = geracaoRepository.buscarUltimaGeracao(data);
        return new GeracaoDiariaDTO(geracoes, gerado);
    }

    @Override
    @Transactional
    public void salvar(GeradoDTO dto) {
        var geracao = Geracao.builder().data(LocalDateTime.now()).geracao(dto.getGerado()).potencia(dto.getPotencia()).build();
        geracaoRepository.save(geracao);
        salvarGeracaoDiaria(geracao);
        salvarGeracaoMensal(geracao);
    }

    public void salvarGeracaoDiaria(Geracao geracao){
        var geracaoDiaria = GeracaoDiaria.builder().geracao(geracao.getGeracao()).data(geracao.getData().toLocalDate()).build();
        geracaoDiariaService.salvar(geracaoDiaria);
    }

    public void salvarGeracaoMensal(Geracao geracao){
        geracaoMensalService.salvar(geracao);
    }

}