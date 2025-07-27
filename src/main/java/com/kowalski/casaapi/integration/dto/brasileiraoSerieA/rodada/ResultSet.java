package com.kowalski.casaapi.integration.dto.brasileiraoSerieA.rodada;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class ResultSet {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate first;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate last;
}
