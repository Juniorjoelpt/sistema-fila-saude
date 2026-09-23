package br.com.filasaude.service;

import br.com.filasaude.domain.Usuario;
import br.com.filasaude.dto.auth.LoginRequest;
import br.com.filasaude.dto.auth.LoginResponse;
import br.com.filasaude.repository.UsuarioRepository;
import br.com.filasaude.security.JwtService;
import br.com.filasaude.tenancy.TenantContext;
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
    private final AuditoriaService auditoriaService;

    public AuthService(AuthenticationManager authenticationManager,
                        UsuarioRepository usuarioRepository,
                        JwtService jwtService,
                        AuditoriaService auditoriaService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
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
        String token = jwtService.gerarToken(usuario, tenant);

        auditoriaService.registrarComEmail("LOGIN", usuario.getEmail(), null);

        return new LoginResponse(token, usuario.getNome(), usuario.getEmail(), usuario.getPapel().name());
    }
}
