package com.kowalski.casaapi.api.v1.assembler;

import com.kowalski.casaapi.api.v1.dto.RastreamentoDto;
import com.kowalski.casaapi.api.v1.input.RastreamentoInput;
import com.kowalski.casaapi.domain.model.Rastreamento;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RastreamentoAssembler {

    private final ModelMapper modelMapper;

    public Rastreamento to(RastreamentoInput rastreamentoInput){
        return modelMapper.map(rastreamentoInput, Rastreamento.class);
    }

    public RastreamentoDto to(Rastreamento rastreamento) {
        return modelMapper.map(rastreamento, RastreamentoDto.class);
    }

}