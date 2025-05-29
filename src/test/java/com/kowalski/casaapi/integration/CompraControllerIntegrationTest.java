package com.kowalski.casaapi.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.domain.model.Cartao;
import com.kowalski.casaapi.domain.repository.CartaoRepository;
import com.kowalski.casaapi.domain.repository.CompraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class CompraControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private CartaoRepository cartaoRepository;

    private CompraInput compraInput;

    @BeforeEach
    void setUp() {
        compraRepository.deleteAll();
        cartaoRepository.deleteAll();

        // Criar cartão
        Cartao cartao = new Cartao();
        cartao.setNome("NUBANK");
        cartaoRepository.save(cartao);

        compraInput = new CompraInput(
            "Produto Teste",
            BigDecimal.valueOf(1000),
            LocalDate.now(),
            3,
            "João",
            "NUBANK"
        );
    }

    @Test
    @DisplayName("Deve buscar todas as compras - Teste de Integração")
    void buscarTodos() throws Exception {
        mockMvc.perform(get("/v1/compra"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Deve buscar compras por mês e nome - Teste de Integração")
    void buscarPorMesENome() throws Exception {
        String ano = String.valueOf(LocalDate.now().getYear());
        String mes = String.format("%02d", LocalDate.now().getMonthValue());

        mockMvc.perform(get("/v1/compra/{ano}/{mes}/joao/NUBANK/NAO", ano, mes))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.compras").isArray());
    }

    @Test
    @DisplayName("Deve salvar compra com sucesso - Teste de Integração")
    void salvar() throws Exception {
        mockMvc.perform(post("/v1/compra")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(compraInput)))
            .andExpect(status().isOk())
            .andExpect(content().string("Cadastro realizado com sucesso."));
    }

    @Test
    @DisplayName("Deve remover compra com sucesso - Teste de Integração")
    void remover() throws Exception {
        // Primeiro salvar uma compra
        mockMvc.perform(post("/v1/compra")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(compraInput)))
            .andExpect(status().isOk());

        // Pegar o ID da compra salva
        UUID compraId = compraRepository.findAll().get(0).getId();

        // Remover a compra
        mockMvc.perform(delete("/v1/compra/{id}", compraId))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve enviar evento com sucesso - Teste de Integração")
    void buscarMesAtual() throws Exception {
        mockMvc.perform(get("/v1/compra/mes-atual"))
            .andExpect(status().isOk());
    }
} 