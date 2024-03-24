package com.kowalski.casaapi.api.v1.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class RastreamentoInput {

    private String nome;
    private String codigo;
    private String status;

}
