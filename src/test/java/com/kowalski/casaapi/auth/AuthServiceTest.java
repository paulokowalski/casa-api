package com.kowalski.casaapi.auth;

import com.kowalski.casaapi.pessoa.Papel;
import com.kowalski.casaapi.pessoa.Pessoa;
import com.kowalski.casaapi.pessoa.PessoaService;
import com.kowalski.casaapi.pessoa.UsuarioLogado;
import com.kowalski.casaapi.config.security.AuthProperties;
import com.kowalski.casaapi.config.security.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final PessoaService repo = mock(PessoaService.class);
    private final RelogioAjustavel relogio = new RelogioAjustavel(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    private JwtConfig jwtConfig;
    private AuthService service;
    private Pessoa ana;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties(new AuthProperties.Jwt(SEGREDO, 8), null);
        jwtConfig = new JwtConfig(props);
        service = new AuthService(repo, new UsuarioLogado(), encoder, jwtConfig.jwtEncoder(), props, relogio);

        ana = new Pessoa();
        ana.setId(7L);
        ana.setNome("Ana");
        ana.setEmail("ana@exemplo.com");
        ana.setPapel(Papel.USUARIO);
        ana.setSenhaHash(encoder.encode("senha-certa"));
        when(repo.porEmail(anyString())).thenReturn(Optional.empty());
        when(repo.porEmail("ana@exemplo.com")).thenReturn(Optional.of(ana));
    }

    @Test
    void loginCertoGeraTokenComOIdDaPessoa() {
        LoginResponse resp = service.login("ana@exemplo.com", "senha-certa");

        Jwt jwt = jwtConfig.jwtDecoder().decode(resp.token());
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(resp.usuario().papel()).isEqualTo(Papel.USUARIO);
        assertThat(resp.expiraEm()).isEqualTo(relogio.instant().plus(Duration.ofHours(8)));
    }

    @Test
    void senhaErradaEEmailInexistenteDaoAMesmaMensagem() {
        assertThatThrownBy(() -> service.login("ana@exemplo.com", "errada"))
                .isInstanceOf(BadCredentialsException.class).hasMessage("E-mail ou senha incorretos.");
        assertThatThrownBy(() -> service.login("ninguem@exemplo.com", "x"))
                .isInstanceOf(BadCredentialsException.class).hasMessage("E-mail ou senha incorretos.");
    }

    @Test
    void pessoaSemSenhaOuInativaNaoEntra() {
        ana.setAtivo(false);
        assertThatThrownBy(() -> service.login("ana@exemplo.com", "senha-certa")).isInstanceOf(BadCredentialsException.class);

        ana.setAtivo(true);
        ana.setSenhaHash(null);
        assertThatThrownBy(() -> service.login("ana@exemplo.com", "senha-certa")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void cincoErrosBloqueiamPorQuinzeMinutos() {
        for (int i = 0; i < AuthService.MAX_TENTATIVAS; i++) {
            assertThatThrownBy(() -> service.login("ANA@exemplo.com", "errada")).isInstanceOf(BadCredentialsException.class);
        }
        assertThatThrownBy(() -> service.login("ana@exemplo.com", "senha-certa")).isInstanceOf(LockedException.class);

        relogio.avancar(AuthService.BLOQUEIO.plusSeconds(1));
        assertThat(service.login("ana@exemplo.com", "senha-certa").token()).isNotBlank();
    }

    @Test
    void segredoCurtoImpedeASubida() {
        assertThatThrownBy(() -> JwtConfig.chave("curto")).isInstanceOf(IllegalStateException.class);
    }

    static class RelogioAjustavel extends Clock {
        private Instant agora;

        RelogioAjustavel(Instant agora) {
            this.agora = agora;
        }

        void avancar(Duration d) {
            agora = agora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
