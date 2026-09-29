package br.com.filasaude.service;

import br.com.filasaude.dto.superadmin.SuperadminLoginRequest;
import br.com.filasaude.dto.superadmin.SuperadminLoginResponse;
import br.com.filasaude.security.JwtService;
import br.com.filasaude.security.LoginRateLimiter;
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

    private static final String CHAVE_TENANT = "__superadmin__";

    private final SuperadminProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;

    public SuperadminAuthService(SuperadminProperties properties, PasswordEncoder passwordEncoder, JwtService jwtService,
                                  LoginRateLimiter loginRateLimiter) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginRateLimiter = loginRateLimiter;
    }

    public SuperadminLoginResponse login(SuperadminLoginRequest request) {
        // Bug alto de revisão corrigido: login de superadmin (acesso ao
        // painel que controla TODOS os tenants) não tinha nenhum limite de
        // tentativas -- ainda mais crítico que o login de tenant comum.
        loginRateLimiter.verificarNaoBloqueado(CHAVE_TENANT, request.email());

        boolean emailConfere = properties.getEmail().equalsIgnoreCase(request.email());
        boolean senhaConfere = emailConfere && passwordEncoder.matches(request.senha(), properties.getPasswordHash());

        if (!senhaConfere) {
            loginRateLimiter.registrarFalha(CHAVE_TENANT, request.email());
            throw new BadCredentialsException("Credenciais inválidas");
        }

        loginRateLimiter.registrarSucesso(CHAVE_TENANT, request.email());
        String token = jwtService.gerarTokenSuperadmin(properties.getEmail(), properties.getNome());
        return new SuperadminLoginResponse(token, properties.getNome(), properties.getEmail());
    }
}
