package br.com.filasaude.service;

import br.com.filasaude.dto.superadmin.SuperadminLoginRequest;
import br.com.filasaude.dto.superadmin.SuperadminLoginResponse;
import br.com.filasaude.security.JwtService;
import br.com.filasaude.security.SuperadminProperties;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Autenticação do painel de superadmin (item 3.6, Fase 2). Credencial única,
 * configurada via {@link SuperadminProperties} — não é um usuário de tenant.
 */
@Service
public class SuperadminAuthService {

    private final SuperadminProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public SuperadminAuthService(SuperadminProperties properties, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public SuperadminLoginResponse login(SuperadminLoginRequest request) {
        boolean emailConfere = properties.getEmail().equalsIgnoreCase(request.email());
        boolean senhaConfere = emailConfere && passwordEncoder.matches(request.senha(), properties.getPasswordHash());

        if (!senhaConfere) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        String token = jwtService.gerarTokenSuperadmin(properties.getEmail(), properties.getNome());
        return new SuperadminLoginResponse(token, properties.getNome(), properties.getEmail());
    }
}
