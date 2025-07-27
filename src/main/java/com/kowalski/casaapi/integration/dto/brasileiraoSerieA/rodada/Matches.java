package com.kowalski.casaapi.integration.dto.brasileiraoSerieA.rodada;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Getter @Setter
public class Matches {

    private Long id;
    private HomeTeam homeTeam;
    private AwayTeam awayTeam;
    private Score score;

    private ZonedDateTime utcDate;

    public LocalDateTime getData() {
        return utcDate.withZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
                .toLocalDateTime();
    }

}
