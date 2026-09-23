package br.com.filasaude.service;

import br.com.filasaude.domain.Usuario;
import br.com.filasaude.dto.twofactor.TwoFactorSetupResponse;
import br.com.filasaude.dto.twofactor.TwoFactorStatusResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.UsuarioRepository;
import br.com.filasaude.security.TotpService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autoatendimento de 2FA para o próprio usuário logado (ativar, confirmar,
 * desativar) -- diferente de AuthService, que cuida do login em si. Toda
 * ação aqui é sobre a conta de quem está autenticado na requisição; não
 * existe endpoint para um usuário mexer no 2FA de outro.
 */
@Service
@Transactional
public class TwoFactorService {

    private final UsuarioRepository usuarioRepository;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public TwoFactorService(UsuarioRepository usuarioRepository, TotpService totpService,
                             PasswordEncoder passwordEncoder, AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.totpService = totpService;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public TwoFactorStatusResponse status() {
        return new TwoFactorStatusResponse(usuarioLogado().isTwoFactorEnabled());
    }

    /**
     * Gera um novo segredo e devolve o QR Code -- ainda NÃO habilita o 2FA
     * (só acontece em {@link #confirmar}, depois de provar que o app
     * autenticador foi configurado corretamente). Chamar de novo antes de
     * confirmar simplesmente substitui o segredo pendente anterior.
     */
    public TwoFactorSetupResponse iniciarConfiguracao() {
        Usuario usuario = usuarioLogado();
        String secret = totpService.gerarSecret();
        usuario.setTwoFactorSecret(secret);
        usuarioRepository.save(usuario);

        String qrCodeBase64 = totpService.gerarQrCodeBase64(secret, usuario.getEmail(), "Fila Saúde");
        return new TwoFactorSetupResponse(secret, qrCodeBase64);
    }

    /** Confirma a configuração validando um código gerado pelo app -- só então o 2FA passa a valer no login. */
    public void confirmar(String codigo) {
        Usuario usuario = usuarioLogado();
        if (usuario.getTwoFactorSecret() == null) {
            throw new IllegalStateException("Nenhuma configuração de 2FA pendente. Inicie a configuração novamente.");
        }
        if (!totpService.validarCodigo(usuario.getTwoFactorSecret(), codigo)) {
            throw new IllegalStateException("Código inválido. Confira o horário do celular e tente novamente.");
        }
        usuario.setTwoFactorEnabled(true);
        usuarioRepository.save(usuario);
        auditoriaService.registrar("2FA_ATIVADO", "Usuario", usuario.getId(),
                "Autenticação em dois fatores ativada para " + usuario.getEmail());
    }

    /** Exige a senha de novo -- ver TwoFactorDesabilitarRequest. */
    public void desabilitar(String senha) {
        Usuario usuario = usuarioLogado();
        if (!passwordEncoder.matches(senha, usuario.getPassword())) {
            throw new IllegalStateException("Senha incorreta.");
        }
        usuario.setTwoFactorEnabled(false);
        usuario.setTwoFactorSecret(null);
        usuarioRepository.save(usuario);
        auditoriaService.registrar("2FA_DESATIVADO", "Usuario", usuario.getId(),
                "Autenticação em dois fatores desativada para " + usuario.getEmail());
    }

    private Usuario usuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmailIgnoreCaseAndAtivoTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + email));
    }
}
