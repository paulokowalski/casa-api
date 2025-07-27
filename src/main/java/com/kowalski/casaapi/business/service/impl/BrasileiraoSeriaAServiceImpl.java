package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.response.RodadaResponse;
import com.kowalski.casaapi.api.v1.response.TabelaResponse;
import com.kowalski.casaapi.business.service.BrasileiraoSeriaAService;
import com.kowalski.casaapi.integration.api.BrasileiraoSeriaAApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrasileiraoSeriaAServiceImpl implements BrasileiraoSeriaAService {

    @Value("${futebol.api.token}")
    private String authToken;

    private final BrasileiraoSeriaAApi brasileiraoSeriaAApi;

    @Override
    public TabelaResponse consultarPosicao(Long idTime) {
        Objects.requireNonNull(idTime, "ID do time não pode ser nulo");

        var tabela = brasileiraoSeriaAApi.tabela(authToken);

        return tabela.getStandings().get(0).getTable().stream()
                .filter(r -> r.getTeam().getId().equals(idTime))
                .findFirst()
                .map(time -> new TabelaResponse(
                        time.getTeam().getName(),
                        time.getTeam().getCrest(),
                        time.getPosition(),
                        time.getPoints(),
                        time.getWon(),
                        time.getDraw(),
                        time.getLost(),
                        time.getGoalsFor(),
                        time.getGoalsAgainst(),
                        time.getGoalDifference()))
                .orElseThrow(() -> new RuntimeException("Time não encontrado na tabela com ID: " + idTime));
    }

    public RodadaResponse consultarRodada(Long idTime) {
        Objects.requireNonNull(idTime, "ID do time não pode ser nulo");

        var tabela = brasileiraoSeriaAApi.tabela(authToken);
        int rodadaAtual = tabela.getSeason().getCurrentMatchday();
        var rodadas = brasileiraoSeriaAApi.consultarRodadas(rodadaAtual, authToken);

        var jogo = rodadas.getMatches().stream()
                .filter(r -> r.getAwayTeam().getId().equals(idTime) || r.getHomeTeam().getId().equals(idTime))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Time não encontrado na rodada atual com ID: " + idTime));

        return new RodadaResponse(
                rodadaAtual,
                jogo.getData(),
                jogo.getHomeTeam().getName(),
                jogo.getHomeTeam().getCrest(),
                jogo.getScore().getFullTime().getHome(),
                jogo.getAwayTeam().getName(),
                jogo.getAwayTeam().getCrest(),
                jogo.getScore().getFullTime().getAway());
    }
}
