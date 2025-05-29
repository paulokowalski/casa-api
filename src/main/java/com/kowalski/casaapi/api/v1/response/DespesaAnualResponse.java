package com.kowalski.casaapi.api.v1.response;

import java.util.Map;

public record DespesaAnualResponse(
    String pessoa,
    String ano,
    Map<String, DespesaResponse> despesasPorMes
) { } 