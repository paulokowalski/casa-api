package com.kowalski.casaapi.api.v1.response;

public record TabelaResponse (String nomeTime, String imagem, int posicao, int pontos, int vitorias, int empates, int derrotas, int golsPro, int golsContra, int saldoGols) { }
