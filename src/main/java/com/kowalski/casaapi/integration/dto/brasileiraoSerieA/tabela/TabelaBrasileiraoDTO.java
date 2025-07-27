package com.kowalski.casaapi.integration.dto.brasileiraoSerieA.tabela;

import com.kowalski.casaapi.integration.dto.brasileiraoSerieA.Filters;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter @Getter
public class TabelaBrasileiraoDTO {

    private Filters filters;
    private SeasonDTO season;
    private List<StandingsDTO> standings;

}