package com.kowalski.casaapi.listener;

import com.kowalski.casaapi.domain.service.CompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecarregaCartaoListener {

    private final CompraService compraService;

    @Transactional
    @KafkaListener(topics = "recarrega-cartao-topic")
    public void geracaoDiaria() {
        compraService.enviarEvento();
    }

}