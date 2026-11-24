package com.kowalski.casaapi.comum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerValidacaoTest {

    record Cadastro(@NotBlank(message = "O nome é obrigatório") String nome) {
    }

    @RestController
    static class ControllerComValidacao {

        @PostMapping("/cadastro")
        void cadastrar(@Valid @RequestBody Cadastro cadastro) {
        }

        @GetMapping("/parcelas")
        void parcelas(@RequestParam @Min(value = 1, message = "Informe ao menos uma parcela") int quantidade) {
        }

        @DeleteMapping("/pessoa")
        void excluirPessoaComRegistros() {
            throw new DataIntegrityViolationException("violates foreign key constraint compra_pessoa_id_fkey");
        }
    }

    private final MockMvc mvc = mvc();

    private static MockMvc mvc() {
        var validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(new ControllerComValidacao())
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validador)
                .build();
    }

    @Test
    void corpoInvalidoRespondeAMensagemDoCampo() throws Exception {
        mvc.perform(post("/cadastro").contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O nome é obrigatório"));
    }

    @Test
    void recusaDoBancoPorIntegridadeResponde409SemDetalhesDoBanco() throws Exception {
        mvc.perform(delete("/pessoa"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Não foi possível concluir: o registro está ligado a outros dados."));
    }

    @Test
    void parametroInvalidoRespondeAMensagemDaRegra() throws Exception {
        mvc.perform(get("/parcelas").param("quantidade", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe ao menos uma parcela"));
    }
}
