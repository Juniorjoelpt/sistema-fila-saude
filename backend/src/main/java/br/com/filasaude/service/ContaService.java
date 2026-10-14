package br.com.filasaude.service;

import br.com.filasaude.domain.Usuario;
import br.com.filasaude.dto.auth.LoginResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.UsuarioRepository;
import br.com.filasaude.security.JwtService;
import br.com.filasaude.security.LoginRateLimiter;
import br.com.filasaude.tenancy.TenantContext;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autoatendimento da própria conta: troca de senha. Antes não existia
 * nenhuma forma de o usuário trocar a senha -- a "senha provisória" do
 * cadastro de equipe era permanente. Só atua sobre quem está autenticado
 * (nunca recebe um id de usuário do cliente), como {@link TwoFactorService}.
 */
@Service
@Transactional
public class ContaService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;

    public ContaService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                        AuditoriaService auditoriaService, JwtService jwtService,
                        LoginRateLimiter loginRateLimiter) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.jwtService = jwtService;
        this.loginRateLimiter = loginRateLimiter;
    }

    /**
     * Troca a senha e devolve uma sessão nova (token sem a marca de senha
     * provisória), para o usuário seguir logado sem precisar entrar de novo.
     */
    public LoginResponse alterarSenha(String senhaAtual, String novaSenha) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCaseAndAtivoTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + email));
        String tenant = TenantContext.getCurrentTenant();

        // Mesmo limite de tentativas do login: sem isso, esta rota permitiria
        // adivinhar a senha atual de uma sessão roubada sem bloqueio.
        loginRateLimiter.verificarNaoBloqueado(tenant, email);

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            loginRateLimiter.registrarFalha(tenant, email);
            throw new IllegalStateException("A senha atual está incorreta.");
        }
        loginRateLimiter.registrarSucesso(tenant, email);

        validarNovaSenha(novaSenha, senhaAtual);

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuario.setTrocarSenha(false);
        usuarioRepository.save(usuario);
        auditoriaService.registrar("ALTERAR_SENHA", "Usuario", usuario.getId(),
                "Senha alterada pelo próprio usuário " + usuario.getEmail());

        String token = jwtService.gerarToken(usuario, tenant);
        return LoginResponse.concluido(token, usuario.getNome(), usuario.getEmail(),
                usuario.getPapel().name(), false);
    }

    static void validarNovaSenha(String novaSenha, String senhaAtual) {
        if (novaSenha.length() < 8) {
            throw new IllegalStateException("A nova senha deve ter ao menos 8 caracteres.");
        }
        boolean temLetra = novaSenha.chars().anyMatch(Character::isLetter);
        boolean temDigito = novaSenha.chars().anyMatch(Character::isDigit);
        if (!temLetra || !temDigito) {
            throw new IllegalStateException("A nova senha deve ter letras e números.");
        }
        if (novaSenha.equals(senhaAtual)) {
            throw new IllegalStateException("A nova senha deve ser diferente da senha atual.");
        }
    }
}
