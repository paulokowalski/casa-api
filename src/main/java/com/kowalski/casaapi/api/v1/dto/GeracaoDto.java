package com.kowalski.casaapi.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class GeracaoDto {

    private float geracao;
    private LocalDateTime dataGeracao;

}
