package com.kowalski.casaapi.api.v1.response;

import java.time.Month;
import java.time.Year;
import java.util.List;

public record GeracaoMensalResponse(List<GeracaoResponse> valores, Year ano, Month mes) { }
