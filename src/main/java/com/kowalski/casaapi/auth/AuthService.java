package com.kowalski.casaapi.auth;

import com.kowalski.casaapi.pessoa.Pessoa;
import com.kowalski.casaapi.pessoa.PessoaService;
import com.kowalski.casaapi.pessoa.UsuarioLogado;
import com.kowalski.casaapi.config.security.AuthProperties;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    static final int MAX_TENTATIVAS = 5;
    static final Duration BLOQUEIO = Duration.ofMinutes(15);
    private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha incorretos.";

    private final PessoaService pessoaService;
    private final UsuarioLogado usuarioLogado;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration validade;
    private final Clock clock;

    private final Map<String, Tentativas> tentativas = new ConcurrentHashMap<>();

    private record Tentativas(int erros, Instant bloqueadoAte) {
    }

    public AuthService(PessoaService pessoaService, UsuarioLogado usuarioLogado, PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder, AuthProperties props, Clock clock) {
        this.pessoaService = pessoaService;
        this.usuarioLogado = usuarioLogado;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        Integer horas = props.jwt() == null ? null : props.jwt().expiracaoHoras();
        this.validade = Duration.ofHours(horas == null || horas <= 0 ? 8 : horas);
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String senha) {
        if (email == null || email.isBlank() || senha == null || senha.isEmpty()) {
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }
        String chave = email.trim().toLowerCase(Locale.ROOT);
        Instant agora = clock.instant();

        Tentativas t = tentativas.get(chave);
        if (t != null && t.bloqueadoAte() != null && agora.isBefore(t.bloqueadoAte())) {
            throw new LockedException("Muitas tentativas erradas. Tente de novo em 15 minutos.");
        }

        Pessoa pessoa = pessoaService.porEmail(email)
                .filter(p -> p.isAtivo() && p.getSenhaHash() != null)
                .filter(p -> passwordEncoder.matches(senha, p.getSenhaHash()))
                .orElse(null);

        if (pessoa == null) {
            registrarErro(chave, agora);
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }
        tentativas.remove(chave);

        Instant expira = agora.plus(validade);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("casa-api")
                .subject(String.valueOf(pessoa.getId()))
                .issuedAt(agora)
                .expiresAt(expira)
                .claim("papel", pessoa.getPapel().name())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        return new LoginResponse(token, expira, UsuarioResponse.de(pessoa));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse usuarioAtual() {
        return pessoaService.porId(usuarioLogado.atual().getPessoaId())
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new BadCredentialsException("Sessão inválida"));
    }

    private void registrarErro(String chave, Instant agora) {
        tentativas.compute(chave, (k, atual) -> {
            int erros = (atual == null || (atual.bloqueadoAte() != null && !agora.isBefore(atual.bloqueadoAte())))
                    ? 1 : atual.erros() + 1;
            return new Tentativas(erros, erros >= MAX_TENTATIVAS ? agora.plus(BLOQUEIO) : null);
        });
    }
}
