package br.com.filasaude.service;

import br.com.filasaude.domain.LogAuditoria;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.dto.auditoria.LogAuditoriaResponse;
import br.com.filasaude.dto.common.PageResponse;
import br.com.filasaude.repository.LogAuditoriaRepository;
import br.com.filasaude.repository.UsuarioRepository;
import br.com.filasaude.specification.LogAuditoriaSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Log de auditoria geral do sistema (item 3.5 do levantamento de
 * requisitos): registro amplo de ações -- login, criação/edição de
 * cadastros, pacientes, protocolos, usuários -- além do que já existe
 * especificamente para status/prioridade de protocolo e ajuste de cota.
 *
 * Chamado a partir dos demais services nos pontos de escrita relevantes.
 * Nunca lança exceção para o chamador: uma falha ao registrar auditoria não
 * pode impedir a ação de negócio em si.
 */
@Service
@Transactional
public class AuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(LogAuditoriaRepository logAuditoriaRepository, UsuarioRepository usuarioRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public void registrar(String acao, String entidade, Long entidadeId, String detalhe) {
        try {
            Usuario usuario = usuarioLogado().orElse(null);
            String email = usuario != null ? usuario.getEmail() : emailAutenticado().orElse(null);

            logAuditoriaRepository.save(LogAuditoria.builder()
                    .usuario(usuario)
                    .usuarioEmail(email)
                    .acao(acao)
                    .entidade(entidade)
                    .entidadeId(entidadeId)
                    .detalhe(detalhe)
                    .build());
        } catch (Exception ignorada) {
            // O log de auditoria é best-effort: uma falha aqui (ex.: banco
            // indisponível) nunca deve impedir a ação de negócio que o chamou.
        }
    }

    /** Variante para tentativas de login, em que o e-mail é conhecido mas pode não haver usuário resolvido. */
    public void registrarComEmail(String acao, String emailInformado, String detalhe) {
        try {
            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailInformado).orElse(null);
            logAuditoriaRepository.save(LogAuditoria.builder()
                    .usuario(usuario)
                    .usuarioEmail(emailInformado)
                    .acao(acao)
                    .detalhe(detalhe)
                    .build());
        } catch (Exception ignorada) {
            // best-effort -- ver comentário em registrar(...)
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<LogAuditoriaResponse> listar(String acao, Long usuarioId, LocalDate dataInicio,
                                                       LocalDate dataFim, int page, int size) {
        Specification<LogAuditoria> spec = Specification
                .where(LogAuditoriaSpecifications.comAcao(acao))
                .and(LogAuditoriaSpecifications.comUsuario(usuarioId))
                .and(LogAuditoriaSpecifications.apartirDe(dataInicio))
                .and(LogAuditoriaSpecifications.ate(dataFim));

        var pagina = logAuditoriaRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by("criadoEm").descending()));

        return PageResponse.de(
                pagina.getContent().stream().map(LogAuditoriaResponse::de).toList(),
                page, size, pagina.getTotalElements());
    }

    private Optional<Usuario> usuarioLogado() {
        return emailAutenticado().flatMap(usuarioRepository::findByEmailIgnoreCase);
    }

    private Optional<String> emailAutenticado() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName);
    }
}
