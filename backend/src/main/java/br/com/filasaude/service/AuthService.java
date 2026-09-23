package br.com.filasaude.service;

import br.com.filasaude.domain.Usuario;
import br.com.filasaude.dto.auth.LoginRequest;
import br.com.filasaude.dto.auth.LoginResponse;
import br.com.filasaude.dto.auth.TwoFactorLoginRequest;
import br.com.filasaude.repository.UsuarioRepository;
import br.com.filasaude.security.JwtService;
import br.com.filasaude.security.TotpService;
import br.com.filasaude.tenancy.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final AuditoriaService auditoriaService;

    public AuthService(AuthenticationManager authenticationManager,
                        UsuarioRepository usuarioRepository,
                        JwtService jwtService,
                        TotpService totpService,
                        AuditoriaService auditoriaService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.totpService = totpService;
        this.auditoriaService = auditoriaService;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.senha()));
        } catch (AuthenticationException e) {
            auditoriaService.registrarComEmail("LOGIN_FALHA", request.email(), "Tentativa de login com credenciais inválidas");
            throw e;
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCaseAndAtivoTrue(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        String tenant = TenantContext.getCurrentTenant();

        // Senha correta, mas 2FA habilitado: ainda não é um login completo --
        // devolve só um token de vida curta (ver JwtService.gerarTokenPreAuth),
        // que o frontend troca pelo token final em /api/auth/2fa/validar-login
        // assim que o usuário digitar o código do app autenticador.
        if (usuario.isTwoFactorEnabled()) {
            String loginToken = jwtService.gerarTokenPreAuth(usuario, tenant);
            auditoriaService.registrarComEmail("LOGIN_2FA_PENDENTE", usuario.getEmail(),
                    "Senha correta, aguardando código do segundo fator");
            return LoginResponse.desafio2fa(loginToken);
        }

        String token = jwtService.gerarToken(usuario, tenant);
        auditoriaService.registrarComEmail("LOGIN", usuario.getEmail(), null);
        return LoginResponse.concluido(token, usuario.getNome(), usuario.getEmail(), usuario.getPapel().name());
    }

    /** Segunda etapa do login: troca o loginToken (pré-2FA) + código do app autenticador pelo token final. */
    public LoginResponse validarDoisFatoresLogin(TwoFactorLoginRequest request) {
        Claims claims;
        try {
            claims = jwtService.validarEExtrairClaims(request.loginToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Login expirado, faça login novamente.");
        }

        boolean pre2fa = Boolean.TRUE.equals(claims.get(JwtService.CLAIM_PRE_2FA, Boolean.class));
        String tenantDoToken = claims.get(JwtService.CLAIM_TENANT, String.class);
        String email = claims.getSubject();

        if (!pre2fa || tenantDoToken == null || !tenantDoToken.equals(TenantContext.getCurrentTenant())) {
            throw new BadCredentialsException("Login expirado, faça login novamente.");
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCaseAndAtivoTrue(email)
                .orElseThrow(() -> new BadCredentialsException("Login expirado, faça login novamente."));

        if (!usuario.isTwoFactorEnabled() || usuario.getTwoFactorSecret() == null
                || !totpService.validarCodigo(usuario.getTwoFactorSecret(), request.codigo())) {
            auditoriaService.registrarComEmail("LOGIN_2FA_FALHA", usuario.getEmail(), "Código do segundo fator inválido");
            throw new BadCredentialsException("Código inválido.");
        }

        String token = jwtService.gerarToken(usuario, tenantDoToken);
        auditoriaService.registrarComEmail("LOGIN", usuario.getEmail(), "Concluído com segundo fator");
        return LoginResponse.concluido(token, usuario.getNome(), usuario.getEmail(), usuario.getPapel().name());
    }
}
