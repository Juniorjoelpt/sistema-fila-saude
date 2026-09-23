package br.com.filasaude.dto.auditoria;

import br.com.filasaude.domain.LogAuditoria;

import java.time.LocalDateTime;

public record LogAuditoriaResponse(
        Long id,
        String usuarioNome,
        String usuarioEmail,
        String acao,
        String entidade,
        Long entidadeId,
        String detalhe,
        LocalDateTime criadoEm
) {
    public static LogAuditoriaResponse de(LogAuditoria log) {
        return new LogAuditoriaResponse(
                log.getId(),
                log.getUsuario() != null ? log.getUsuario().getNome() : null,
                log.getUsuarioEmail(),
                log.getAcao(),
                log.getEntidade(),
                log.getEntidadeId(),
                log.getDetalhe(),
                log.getCriadoEm()
        );
    }
}
