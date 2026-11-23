package com.kowalski.casaapi.auth;

import com.kowalski.casaapi.pessoa.PessoaService;
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
    private final PessoaService pessoaService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var admin = props.admin();
        if (admin == null || admin.email() == null || admin.email().isBlank()
                || admin.senha() == null || admin.senha().isBlank()) {
            return;
        }
        pessoaService.porEmail(admin.email()).ifPresentOrElse(pessoa -> {
            if (pessoa.getSenhaHash() != null) return;
            pessoaService.definirComoAdmin(pessoa, admin.senha());
            log.info("Admin inicial configurado para a pessoa {}", pessoa.getId());
        }, () -> log.warn("AUTH_ADMIN_EMAIL não corresponde a nenhuma pessoa cadastrada"));
    }
}
