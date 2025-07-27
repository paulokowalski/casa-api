package com.kowalski.casaapi.integration.dto.brasileiraoSerieA.tabela;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TableDTO {

    private TeamDTO team;
    private int position;
    private int won;
    private int draw;
    private int lost;
    private int points;
    private int goalsFor;
    private int goalsAgainst;
    private int goalDifference;
}
