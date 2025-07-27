package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.response.RodadaResponse;
import com.kowalski.casaapi.api.v1.response.TabelaResponse;
import com.kowalski.casaapi.business.service.BrasileiraoSeriaAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/brasileirao-serie-a")
public class BrasileiraoSeriaAController {

    private final BrasileiraoSeriaAService brasileiraoSeriaAService;

    @GetMapping("/posicao/{idTime}")
    public ResponseEntity<TabelaResponse> consultarPosicao(@PathVariable Long idTime) {
        return ResponseEntity.ok(brasileiraoSeriaAService.consultarPosicao(idTime));
    }

    @GetMapping("/rodada-atual/{idTime}")
    public ResponseEntity<RodadaResponse> consultarRodadas(@PathVariable Long idTime) {
        return ResponseEntity.ok(brasileiraoSeriaAService.consultarRodada(idTime));
    }

}
