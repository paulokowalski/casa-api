package com.kowalski.casaapi.api.v1.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.Month;
import java.time.Year;
import java.util.List;

public record GeracaoMensalResponse(List<GeracaoResponse> valores,
                                    @JsonSerialize(using = ToStringSerializer.class) Year ano,
                                    @JsonSerialize(using = ToStringSerializer.class) Month mes) { }
