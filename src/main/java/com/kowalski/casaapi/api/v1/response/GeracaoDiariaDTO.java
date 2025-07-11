package com.kowalski.casaapi.api.v1.response;

import java.util.List;

public record GeracaoDiariaDTO(List<GeracaoDTO> potencias, Double geracao) { }
