package br.com.filasaude.controller;

import br.com.filasaude.dto.auth.LoginRequest;
import br.com.filasaude.dto.auth.LoginResponse;
import br.com.filasaude.dto.auth.TwoFactorLoginRequest;
import br.com.filasaude.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Segunda etapa do login quando a conta tem 2FA habilitado (ver LoginResponse.requerDoisFatores). */
    @PostMapping("/2fa/validar-login")
    public LoginResponse validarDoisFatoresLogin(@Valid @RequestBody TwoFactorLoginRequest request) {
        return authService.validarDoisFatoresLogin(request);
    }
}
