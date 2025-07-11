package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.dto.GeradoDTO;
import com.kowalski.casaapi.api.v1.response.GeracaoDTO;
import com.kowalski.casaapi.api.v1.response.GeracaoDiariaDTO;
import com.kowalski.casaapi.business.model.Geracao;
import com.kowalski.casaapi.business.repository.GeracaoRepository;
import com.kowalski.casaapi.business.service.GeracaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GeracaoServiceImpl implements GeracaoService {

    private final GeracaoRepository geracaoRepository;

    @Override
    public GeracaoDiariaDTO buscarGeracao(LocalDate data) {
        var geracoes = geracaoRepository.buscarPorData(data).stream().map(ger -> new GeracaoDTO(ger.getPotencia(), ger.getData())).toList();
        var gerado = geracaoRepository.buscarUltimaGeracao(data);
        return new GeracaoDiariaDTO(geracoes, gerado);
    }

    @Override
    public void salvar(GeradoDTO dto) {
        var geracao = Geracao.builder().data(LocalDateTime.now()).geracao(dto.getGerado()).potencia(dto.getPotencia()).build();
        geracaoRepository.save(geracao);
    }

}