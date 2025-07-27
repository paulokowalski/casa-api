package com.kowalski.casaapi.integration.dto.brasileiraoSerieA.tabela;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class StandingsDTO {

    private String stage;

    List<TableDTO> table;

}
