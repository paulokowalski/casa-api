package com.kowalski.casaapi.api.v1.dto;

import java.util.List;

public class DatasetDto {
        private List<Double> data;

    public DatasetDto(List<Double> data) {
        this.data = data;
    }

    public List<Double> getData() {
        return data;
    }
    }