package com.kowalski.casaapi.api.v1.assembler;

import com.kowalski.casaapi.api.v1.dto.GeracaoDto;
import com.kowalski.casaapi.domain.model.Geracao;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeracaoAssembler {

    private final ModelMapper modelMapper;

    public GeracaoDto to(Geracao geracao) {
        return modelMapper.map(geracao, GeracaoDto.class);
    }

}