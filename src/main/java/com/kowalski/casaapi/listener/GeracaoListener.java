package com.kowalski.casaapi.listener;

import com.kowalski.casaapi.domain.model.GeracaoDiaria;
import com.kowalski.casaapi.domain.service.GeracaoDiariaService;
import com.kowalski.casaapi.domain.service.GeracaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GeracaoListener {

    private final KafkaTemplate<String, Serializable> kafkaTemplate;
    private final GeracaoService geracaoService;
    private final GeracaoDiariaService geracaoDiariaService;

    @Transactional
    @KafkaListener(topics = "geracao-diaria-topic")
    public void geracaoDiaria(String mensagem) {
        var day = String.valueOf(LocalDate.now().getDayOfMonth());
        var month = String.valueOf(LocalDate.now().getMonth().getValue());
        var year = String.valueOf(LocalDate.now().getYear());
        var geracao = geracaoService.buscarGeracaoPorDiaMes(day, month, year);
        if(Objects.nonNull(geracao)){
            geracaoDiariaService.salvar(GeracaoDiaria.builder().dataGeracao(geracao.getDataCadastro().toLocalDate()).geracao(geracao.getGeracao()).build());
        }
        kafkaTemplate.send("geracao-diaria-ha-topic", geracao.getGeracao());
        kafkaTemplate.send("geracao-mensal-ha-topic", geracaoDiariaService.buscarGeracaoMensal(month, year));
    }
}
