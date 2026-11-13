package com.kowalski.casaapi.api.v1.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.Month;
import java.time.Year;

public record GeracaoMesAnoResponse(@JsonSerialize(using = ToStringSerializer.class) Month month,
                                    @JsonSerialize(using = ToStringSerializer.class) Year year,
                                    Double value) { }
