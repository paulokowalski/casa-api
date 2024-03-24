package com.kowalski.casaapi.config.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeracaoDiariaEvent implements Serializable {

    private String dia;

    private String mes;

    private String ano;

}
