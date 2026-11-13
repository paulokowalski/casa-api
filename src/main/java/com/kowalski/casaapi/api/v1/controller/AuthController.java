package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.input.LoginInput;
import com.kowalski.casaapi.api.v1.input.TrocaSenhaInput;
import com.kowalski.casaapi.api.v1.response.LoginResponse;
import com.kowalski.casaapi.api.v1.response.UsuarioResponse;
import com.kowalski.casaapi.business.auth.AuthService;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.business.repository.PessoaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthService authService;
    private final UsuarioLogado usuarioLogado;
    private final PessoaRepository pessoaRepository;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginInput input) {
        return authService.login(input.email(), input.senha());
    }

    @GetMapping("/me")
    public UsuarioResponse me() {
        return pessoaRepository.findById(usuarioLogado.atual().getPessoaId())
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new BadCredentialsException("Sessão inválida"));
    }

    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@RequestBody TrocaSenhaInput input) {
        authService.trocarSenha(usuarioLogado.atual().getPessoaId(), input.senhaAtual(), input.novaSenha());
    }
}
