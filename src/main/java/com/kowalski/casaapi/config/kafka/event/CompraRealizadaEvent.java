package com.kowalski.casaapi.config.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompraRealizadaEvent implements Event {

    private String ano;
    private String mes;
    private String pessoa;
    private BigDecimal valor;

    @Override
    public String getEventType() {
        return "COMPRA_REALIZADA";
    }
} 