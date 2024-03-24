package com.kowalski.casaapi.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class RastreamentoDto {

    private UUID id;

    private String nome;

    private String codigo;

    private String status;

}