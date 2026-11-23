package com.kowalski.casaapi.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Autenticação", description = "Login e dados do usuário logado")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Entrar com e-mail e senha",
            description = "Devolve o token JWT (válido por 8 horas por padrão) e os dados do usuário. "
                    + "Após 5 senhas erradas seguidas, o e-mail fica bloqueado por 15 minutos.")
    @ApiResponse(responseCode = "200", description = "Login feito")
    @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos, ou pessoa sem acesso/inativa")
    @ApiResponse(responseCode = "429", description = "Muitas tentativas erradas; tente de novo em 15 minutos")
    public LoginResponse login(@RequestBody LoginInput input) {
        return authService.login(input.email(), input.senha());
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário logado", description = "Usado pelo front para validar a sessão salva.")
    public UsuarioResponse me() {
        return authService.usuarioAtual();
    }
}
