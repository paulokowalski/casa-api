package com.kowalski.casaapi.api.v1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/financeiro", produces = MediaType.APPLICATION_JSON_VALUE)
public class FinanceiroController {
}
