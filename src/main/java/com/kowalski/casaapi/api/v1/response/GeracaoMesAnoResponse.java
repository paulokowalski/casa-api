package com.kowalski.casaapi.api.v1.response;

import java.time.Month;
import java.time.Year;

public record GeracaoMesAnoResponse(Month month, Year year, Double value) { }
