package com.kowalski.casaapi.config;

import com.kowalski.casaapi.api.v1.controller.AuthController;
import com.kowalski.casaapi.compra.CompraController;
import com.kowalski.casaapi.business.auth.AdminInicial;
import com.kowalski.casaapi.business.auth.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.kowalski.casaapi.geracao.GeracaoController;
import com.kowalski.casaapi.pessoa.PessoaController;
import com.kowalski.casaapi.api.v1.controller.ServidorController;
import com.kowalski.casaapi.ventilacao.VentilacaoController;
import com.kowalski.casaapi.ventilacao.VentilacaoService;
import com.kowalski.casaapi.business.monitor.MonitorService;
import com.kowalski.casaapi.comum.ApiExceptionHandler;
import com.kowalski.casaapi.comum.RelogioConfig;
import com.kowalski.casaapi.config.security.ErroSegurancaJson;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.pessoa.Papel;
import com.kowalski.casaapi.pessoa.Pessoa;
import com.kowalski.casaapi.pessoa.PessoaRepository;
import com.kowalski.casaapi.compra.CompraService;
import com.kowalski.casaapi.geracao.GeracaoService;
import com.kowalski.casaapi.pessoa.PessoaService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CompraController.class, PessoaController.class, GeracaoController.class, AuthController.class,
        ServidorController.class, VentilacaoController.class,
        com.kowalski.casaapi.api.v1.controller.PassagensController.class,
        com.kowalski.casaapi.crescimento.exame.ExameController.class})
@Import({RelogioConfig.class, SecurityConfig.class, JwtConfig.class, PessoaJwtConverter.class, ApiKeyAuthFilter.class,
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
    @MockitoBean MonitorService monitorService;
    @MockitoBean VentilacaoService ventilacaoService;
    @MockitoBean com.kowalski.casaapi.business.passagens.PassagensService passagensService;
    @MockitoBean com.kowalski.casaapi.business.passagens.RotasPassagemService rotasPassagemService;
    @MockitoBean com.kowalski.casaapi.crescimento.exame.ExameService exameService;

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
        verify(compraService).buscarTodos(null, null);
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
    void soAdminVeOMonitoramentoDoServidor() throws Exception {
        for (String rota : new String[]{"/v1/servidor/agora", "/v1/servidor/dia", "/v1/servidor/dias"}) {
            mvc.perform(get(rota)).andExpect(status().isUnauthorized());
            mvc.perform(get(rota).header("Authorization", bearer(1L))).andExpect(status().isForbidden());
            mvc.perform(get(rota).header("X-API-KEY", "chave-integracao")).andExpect(status().isForbidden());
            mvc.perform(get(rota).header("Authorization", bearer(3L))).andExpect(status().isOk());
        }
        verify(monitorService).agora();
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
    void chaveDeIntegracaoSoGravaAVentilacao() throws Exception {
        String leitura = "{\"tomada\":\"on\"}";
        mvc.perform(post("/v1/servidor/ventilacao").header("X-API-KEY", "chave-integracao")
                .contentType(MediaType.APPLICATION_JSON).content(leitura)).andExpect(status().isNoContent());
        mvc.perform(get("/v1/servidor/ventilacao").header("X-API-KEY", "chave-integracao")).andExpect(status().isForbidden());
        mvc.perform(get("/v1/servidor/ventilacao/dia").header("X-API-KEY", "chave-integracao")).andExpect(status().isForbidden());
        mvc.perform(post("/v1/servidor/ventilacao").contentType(MediaType.APPLICATION_JSON).content(leitura))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/servidor/ventilacao").header("Authorization", bearer(1L))
                .contentType(MediaType.APPLICATION_JSON).content(leitura)).andExpect(status().isForbidden());
        mvc.perform(get("/v1/servidor/ventilacao").header("Authorization", bearer(3L))).andExpect(status().isOk());
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

    @Test
    void documentacaoSwaggerEPublica() throws Exception {
        // Neste teste de fatia o springdoc não sobe: o importante é a segurança não barrar (não ser 401/403).
        for (String rota : new String[]{"/v3/api-docs", "/swagger-ui.html", "/swagger-ui/index.html"}) {
            int status = mvc.perform(get(rota)).andReturn().getResponse().getStatus();
            assertThat(status).as(rota).isNotIn(401, 403);
        }
    }

    /** O arquivo do exame só sai com login (a liberação sem login dos arquivos de foto não pode pegar esta rota). */
    @Test
    void arquivoDoExameExigeLogin() throws Exception {
        String rota = "/v1/crescimento/criancas/" + java.util.UUID.randomUUID() + "/exames/" + java.util.UUID.randomUUID() + "/arquivo";
        mvc.perform(get(rota)).andExpect(status().isUnauthorized());
        mvc.perform(get(rota).header("X-API-KEY", "chave-integracao")).andExpect(status().isForbidden());
        mvc.perform(get(rota + "?v=ORIGINAL&exp=9999999999&sig=x")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/crescimento/criancas/" + java.util.UUID.randomUUID() + "/exames")).andExpect(status().isUnauthorized());
    }

    @Test
    void passagensSoParaAdmin() throws Exception {
        String rota = "{\"nome\":\"Teste\"}";
        // ADMIN lê, cria, edita e remove
        mvc.perform(get("/v1/passagens").header("Authorization", bearer(3L))).andExpect(status().is2xxSuccessful());
        mvc.perform(post("/v1/passagens/rotas").header("Authorization", bearer(3L))
                .contentType(MediaType.APPLICATION_JSON).content(rota)).andExpect(status().is2xxSuccessful());
        mvc.perform(put("/v1/passagens/rotas/1").header("Authorization", bearer(3L))
                .contentType(MediaType.APPLICATION_JSON).content(rota)).andExpect(status().is2xxSuccessful());
        mvc.perform(delete("/v1/passagens/rotas/1?historico=true").header("Authorization", bearer(3L)))
                .andExpect(status().is2xxSuccessful());

        // USUARIO e a chave de integração não fazem nada
        for (var cabecalho : java.util.List.of(new String[]{"Authorization", bearer(1L)},
                new String[]{"X-API-KEY", "chave-integracao"})) {
            mvc.perform(get("/v1/passagens").header(cabecalho[0], cabecalho[1])).andExpect(status().isForbidden());
            mvc.perform(post("/v1/passagens/rotas").header(cabecalho[0], cabecalho[1])
                    .contentType(MediaType.APPLICATION_JSON).content(rota)).andExpect(status().isForbidden());
            mvc.perform(put("/v1/passagens/rotas/1").header(cabecalho[0], cabecalho[1])
                    .contentType(MediaType.APPLICATION_JSON).content(rota)).andExpect(status().isForbidden());
            mvc.perform(delete("/v1/passagens/rotas/1?historico=true").header(cabecalho[0], cabecalho[1]))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/v1/passagens")).andExpect(status().isUnauthorized());
        org.mockito.Mockito.verify(rotasPassagemService).remover(1L, true);
        org.mockito.Mockito.verify(rotasPassagemService, org.mockito.Mockito.times(1)).remover(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyBoolean());
    }
}
