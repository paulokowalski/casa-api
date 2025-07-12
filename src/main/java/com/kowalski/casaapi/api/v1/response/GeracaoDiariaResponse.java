package com.kowalski.casaapi.api.v1.response;

import java.util.List;

public record GeracaoDiariaResponse(List<GeracaoResponse> valores, Double geracao) { }
