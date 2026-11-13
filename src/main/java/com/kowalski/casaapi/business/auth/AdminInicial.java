package com.kowalski.casaapi.business.auth;

import com.kowalski.casaapi.business.enumerator.Papel;
import com.kowalski.casaapi.business.repository.PessoaRepository;
import com.kowalski.casaapi.config.security.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInicial implements ApplicationRunner {

    private final AuthProperties props;
    private final PessoaRepository pessoaRepository;
    private final AuthService authService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var admin = props.admin();
        if (admin == null || admin.email() == null || admin.email().isBlank()
                || admin.senha() == null || admin.senha().isBlank()) {
            return;
        }
        pessoaRepository.findByEmailIgnoreCase(admin.email().trim()).ifPresentOrElse(pessoa -> {
            if (pessoa.getSenhaHash() != null) return;
            pessoa.setSenhaHash(authService.hash(admin.senha()));
            pessoa.setPapel(Papel.ADMIN);
            pessoa.setAtivo(true);
            pessoaRepository.save(pessoa);
            log.info("Admin inicial configurado para a pessoa {}", pessoa.getId());
        }, () -> log.warn("AUTH_ADMIN_EMAIL não corresponde a nenhuma pessoa cadastrada"));
    }
}
