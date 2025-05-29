package com.kowalski.casaapi.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kowalski.casaapi.domain.model.Compra;
import com.kowalski.casaapi.domain.model.CompraParcela;
import com.kowalski.casaapi.domain.repository.CompraParcelaRepository;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class FiltroControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private CompraParcelaRepository compraParcelaRepository;

    @BeforeEach
    void setUp() {
        compraParcelaRepository.deleteAll();
        compraRepository.deleteAll();

        // Criar uma compra para teste
        Compra compra = Compra.builder()
            .id(UUID.randomUUID())
            .nomeProduto("PRODUTO TESTE")
            .valorProduto(BigDecimal.valueOf(1000))
            .dataCompra(LocalDate.now())
            .numeroParcelas(3)
            .nomePessoaCompra("JOAO")
            .nomeCartao("NUBANK")
            .dataCadastro(LocalDateTime.now())
            .build();
        compraRepository.save(compra);

        // Criar parcelas
        CompraParcela parcela = CompraParcela.builder()
            .id(UUID.randomUUID())
            .compra(compra)
            .numeroParcela(1)
            .valorParcela(BigDecimal.valueOf(333.33))
            .dataParcela(LocalDate.now())
            .build();
        compraParcelaRepository.save(parcela);
    }

    @Test
    @DisplayName("Deve buscar anos - Teste de Integração")
    void buscarAnos() throws Exception {
        mockMvc.perform(get("/v1/filtro/anos"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].codigo").exists())
            .andExpect(jsonPath("$[0].descricao").exists());
    }

    @Test
    @DisplayName("Deve buscar meses - Teste de Integração")
    void buscarMeses() throws Exception {
        String ano = String.valueOf(LocalDate.now().getYear());

        mockMvc.perform(get("/v1/filtro/meses/{ano}", ano))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].codigo").exists())
            .andExpect(jsonPath("$[0].descricao").exists());
    }

    @Test
    @DisplayName("Deve buscar cartões - Teste de Integração")
    void buscarCartoes() throws Exception {
        String ano = String.valueOf(LocalDate.now().getYear());
        String mes = String.format("%02d", LocalDate.now().getMonthValue());

        mockMvc.perform(get("/v1/filtro/cartao/{ano}/{mes}/JOAO", ano, mes))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].codigo").exists())
            .andExpect(jsonPath("$[0].descricao").exists());
    }

    @Test
    @DisplayName("Deve buscar pessoas - Teste de Integração")
    void buscarPessoas() throws Exception {
        String ano = String.valueOf(LocalDate.now().getYear());
        String mes = String.format("%02d", LocalDate.now().getMonthValue());

        mockMvc.perform(get("/v1/filtro/pessoas/{ano}/{mes}", ano, mes))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].codigo").exists())
            .andExpect(jsonPath("$[0].descricao").exists());
    }

    @Test
    @DisplayName("Deve retornar not found quando não encontrar dados para o ano - Teste de Integração")
    void buscarMeses_NotFound() throws Exception {
        mockMvc.perform(get("/v1/filtro/meses/1900"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar not found quando não encontrar dados para cartões - Teste de Integração")
    void buscarCartoes_NotFound() throws Exception {
        mockMvc.perform(get("/v1/filtro/cartao/1900/01/USUARIO_INEXISTENTE"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar not found quando não encontrar dados para pessoas - Teste de Integração")
    void buscarPessoas_NotFound() throws Exception {
        mockMvc.perform(get("/v1/filtro/pessoas/1900/01"))
            .andExpect(status().isNotFound());
    }
} 