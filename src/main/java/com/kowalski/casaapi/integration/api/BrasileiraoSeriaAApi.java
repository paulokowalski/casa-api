package com.kowalski.casaapi.integration.api;

import com.kowalski.casaapi.config.feign.FootballApiConfig;
import com.kowalski.casaapi.integration.dto.brasileiraoSerieA.rodada.FutebolDataDTO;
import com.kowalski.casaapi.integration.dto.brasileiraoSerieA.tabela.TabelaBrasileiraoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "footballDataApi",
        url = "https://api.football-data.org/v4",
        configuration = FootballApiConfig.class
)
public interface BrasileiraoSeriaAApi {

    @GetMapping("/competitions/BSA/standings")
    TabelaBrasileiraoDTO tabela(
            @RequestHeader("X-Auth-Token") String authToken
    );

    @GetMapping("/competitions/BSA/matches")
    FutebolDataDTO consultarRodadas(
            @RequestParam("matchday") int matchday,
            @RequestHeader("X-Auth-Token") String authToken
    );
}