package com.kowalski.casaapi.crescimento;

import com.kowalski.casaapi.comum.ApiExceptionHandler;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.pessoa.PessoaRepository;
import com.kowalski.casaapi.config.ApiKeyAuthFilter;
import com.kowalski.casaapi.config.SecurityConfig;
import com.kowalski.casaapi.config.security.ErroSegurancaJson;
import com.kowalski.casaapi.config.security.JwtConfig;
import com.kowalski.casaapi.config.security.PessoaJwtConverter;
import com.kowalski.casaapi.crescimento.api.CrescimentoExceptionHandler;
import com.kowalski.casaapi.comum.RelogioConfig;
import com.kowalski.casaapi.crescimento.comum.CrescimentoConfig;
import com.kowalski.casaapi.crescimento.comum.SemPermissaoException;
import com.kowalski.casaapi.crescimento.crianca.CriancaController;
import com.kowalski.casaapi.crescimento.crianca.CriancaService;
import com.kowalski.casaapi.crescimento.foto.FotoController;
import com.kowalski.casaapi.crescimento.foto.FotoService;
import com.kowalski.casaapi.crescimento.foto.Variante;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {CriancaController.class, FotoController.class})
@Import({RelogioConfig.class, SecurityConfig.class, JwtConfig.class, PessoaJwtConverter.class, ApiKeyAuthFilter.class, UsuarioLogado.class,
        ApiExceptionHandler.class, CrescimentoExceptionHandler.class, ErroSegurancaJson.class, CrescimentoConfig.class})
@TestPropertySource(properties = {"auth.jwt.secret=segredo-de-teste-com-mais-de-32-caracteres", "api.key=chave-integracao"})
class CrescimentoSegurancaTest {

    @Autowired MockMvc mvc;
    @MockitoBean CriancaService criancaService;
    @MockitoBean FotoService fotoService;
    @MockitoBean PessoaRepository pessoaRepository;

    @Test
    void dadosDasCriancasExigemLogin() throws Exception {
        mvc.perform(get("/v1/crescimento/criancas/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/crescimento/familias/" + UUID.randomUUID() + "/criancas")).andExpect(status().isUnauthorized());
    }

    @Test
    void arquivoDaFotoAbreSemLoginComLinkAssinado() throws Exception {
        UUID id = UUID.randomUUID();
        when(fotoService.arquivo(eq(id), eq(Variante.MINI), anyLong(), eq("ok"))).thenReturn(new ByteArrayResource(new byte[]{1, 2}));
        mvc.perform(get("/v1/crescimento/fotos/" + id + "/arquivo").param("v", "MINI").param("exp", "9999999999").param("sig", "ok"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
    }

    @Test
    void linkInvalidoDa403SemCorpo() throws Exception {
        UUID id = UUID.randomUUID();
        when(fotoService.arquivo(any(), any(), anyLong(), anyString())).thenThrow(new SemPermissaoException("x"));
        mvc.perform(get("/v1/crescimento/fotos/" + id + "/arquivo").param("v", "MINI").param("exp", "1").param("sig", "x"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));
    }
}
