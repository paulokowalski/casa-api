package com.kowalski.casaapi.integration.dto.brasileiraoSerieA.rodada;

import com.kowalski.casaapi.integration.dto.brasileiraoSerieA.Filters;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class FutebolDataDTO {

    private Filters filters;
    private ResultSet resultSet;
    private List<Matches> matches;

}
