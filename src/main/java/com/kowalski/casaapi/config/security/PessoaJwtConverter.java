package com.kowalski.casaapi.config.security;

import com.kowalski.casaapi.business.repository.PessoaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PessoaJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final PessoaRepository pessoaRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("Token inválido");
        }
        return pessoaRepository.findById(id)
                .filter(p -> p.isAtivo() && p.getSenhaHash() != null)
                .map(p -> new UsuarioAutenticado(p.getId(), p.getNome(), p.getPapel()))
                .orElseThrow(() -> new BadCredentialsException("Acesso encerrado"));
    }
}
