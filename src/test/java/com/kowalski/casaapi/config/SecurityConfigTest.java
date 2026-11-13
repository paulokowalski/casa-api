package com.kowalski.casaapi.config;

import com.kowalski.casaapi.api.v1.controller.AuthController;
import com.kowalski.casaapi.api.v1.controller.CompraController;
import com.kowalski.casaapi.business.auth.AdminInicial;
import com.kowalski.casaapi.business.auth.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.kowalski.casaapi.api.v1.controller.GeracaoController;
import com.kowalski.casaapi.api.v1.controller.PessoaController;
import com.kowalski.casaapi.api.v1.handler.ApiExceptionHandler;
import com.kowalski.casaapi.config.security.ErroSegurancaJson;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.business.enumerator.Papel;
import com.kowalski.casaapi.business.model.Pessoa;
import com.kowalski.casaapi.business.repository.PessoaRepository;
import com.kowalski.casaapi.business.service.CompraService;
import com.kowalski.casaapi.business.service.GeracaoService;
import com.kowalski.casaapi.business.service.PessoaService;
import com.kowalski.casaapi.config.security.JwtConfig;
import com.kowalski.casaapi.config.security.PessoaJwtConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CompraController.class, PessoaController.class, GeracaoController.class, AuthController.class})
@Import({SecurityConfig.class, JwtConfig.class, PessoaJwtConverter.class, ApiKeyAuthFilter.class,
        UsuarioLogado.class, ApiExceptionHandler.class, ErroSegurancaJson.class, AuthService.class, AdminInicial.class})
@TestPropertySource(properties = {
        "auth.jwt.secret=segredo-de-teste-com-mais-de-32-caracteres",
        "api.key=chave-integracao"
})
class SecurityConfigTest {

    private static final String COMPRAS_DA_ANA = "/v1/compras/parcelas/2026/11?pessoa=ANA";

    @Autowired MockMvc mvc;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean PessoaRepository pessoaRepository;
    @MockitoBean CompraService compraService;
    @MockitoBean PessoaService pessoaService;
    @MockitoBean GeracaoService geracaoService;

    @BeforeEach
    void pessoas() {
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa(1L, "Ana", Papel.USUARIO, true)));
        when(pessoaRepository.findById(2L)).thenReturn(Optional.of(pessoa(2L, "Bruno", Papel.USUARIO, true)));
        when(pessoaRepository.findById(3L)).thenReturn(Optional.of(pessoa(3L, "Admin", Papel.ADMIN, true)));
        when(pessoaRepository.findById(4L)).thenReturn(Optional.of(pessoa(4L, "Inativo", Papel.USUARIO, false)));
    }

    @Test
    void loginPelaApiDevolveTokenQueFunciona() throws Exception {
        Pessoa ana = pessoa(1L, "Ana", Papel.USUARIO, true);
        ana.setSenhaHash(passwordEncoder.encode("senha-certa"));
        when(pessoaRepository.findByEmailIgnoreCase("ana@exemplo.com")).thenReturn(Optional.of(ana));
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(ana));

        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@exemplo.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized());

        String resposta = mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@exemplo.com\",\"senha\":\"senha-certa\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(resposta, "$.token");

        mvc.perform(get("/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.nome").value("Ana"));
    }

    @Test
    void semTokenResponde401ComMensagem() throws Exception {
        mvc.perform(get(COMPRAS_DA_ANA)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Faça login para continuar."));
    }

    @Test
    void tokenInvalidoResponde401() throws Exception {
        mvc.perform(get(COMPRAS_DA_ANA).header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void semPermissaoResponde403ComMensagem() throws Exception {
        mvc.perform(delete("/v1/pessoas/2").header("Authorization", bearer(1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Você não tem permissão para esta ação."));
    }

    @Test
    void qualquerPessoaLogadaListaCompras() throws Exception {
        mvc.perform(get("/v1/compras").header("Authorization", bearer(1L))).andExpect(status().isOk());
        verify(compraService).buscarTodos();
    }

    @Test
    void pessoaVeAsPropriasCompras() throws Exception {
        mvc.perform(get(COMPRAS_DA_ANA).header("Authorization", bearer(1L))).andExpect(status().isOk());
        verify(compraService).buscarPorMesENome("2026", "11", "ANA", "TODOS", "TODOS");
    }

    @Test
    void pessoaNaoVeComprasDeOutra() throws Exception {
        mvc.perform(get(COMPRAS_DA_ANA).header("Authorization", bearer(2L))).andExpect(status().isForbidden());
        verify(compraService, never()).buscarPorMesENome(any(), any(), any(), any(), any());
    }

    @Test
    void adminVeComprasDeQualquerPessoa() throws Exception {
        mvc.perform(get(COMPRAS_DA_ANA).header("Authorization", bearer(3L))).andExpect(status().isOk());
        verify(compraService).buscarPorMesENome(eq("2026"), eq("11"), eq("ANA"), any(), any());
    }

    @Test
    void pessoaDesativadaPerdeOAcessoNaHora() throws Exception {
        mvc.perform(get(COMPRAS_DA_ANA).header("Authorization", bearer(4L))).andExpect(status().isUnauthorized());
    }

    @Test
    void soAdminCadastraPessoas() throws Exception {
        String corpo = "{\"nome\":\"Nova\",\"email\":\"nova@exemplo.com\"}";
        mvc.perform(post("/v1/pessoas").header("Authorization", bearer(1L))
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
        mvc.perform(delete("/v1/pessoas/2").header("Authorization", bearer(1L))).andExpect(status().isForbidden());
        mvc.perform(post("/v1/pessoas").header("Authorization", bearer(3L))
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated());
    }

    @Test
    void qualquerPessoaLogadaListaPessoas() throws Exception {
        mvc.perform(get("/v1/pessoas").header("Authorization", bearer(1L))).andExpect(status().isOk());
    }

    @Test
    void chaveDeIntegracaoSoEnviaGeracaoSolar() throws Exception {
        mvc.perform(post("/v1/geracao-solar").header("X-API-KEY", "chave-integracao")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
        mvc.perform(get(COMPRAS_DA_ANA).header("X-API-KEY", "chave-integracao")).andExpect(status().isForbidden());
        mvc.perform(get("/v1/pessoas").header("X-API-KEY", "chave-integracao")).andExpect(status().isForbidden());
    }

    @Test
    void chaveErradaNaoAutentica() throws Exception {
        mvc.perform(post("/v1/geracao-solar").header("X-API-KEY", "errada")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    private String bearer(long pessoaId) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(pessoaId)).issuedAt(agora).expiresAt(agora.plusSeconds(600)).build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private static Pessoa pessoa(long id, String nome, Papel papel, boolean ativo) {
        Pessoa p = new Pessoa();
        p.setId(id);
        p.setNome(nome);
        p.setEmail(nome.toLowerCase() + "@exemplo.com");
        p.setPapel(papel);
        p.setAtivo(ativo);
        p.setSenhaHash("hash");
        return p;
    }
}
