package br.com.filasaude.specification;

import br.com.filasaude.domain.LogAuditoria;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class LogAuditoriaSpecifications {

    private LogAuditoriaSpecifications() {
    }

    public static Specification<LogAuditoria> comAcao(String acao) {
        return (root, query, cb) -> (acao == null || acao.isBlank()) ? null : cb.equal(root.get("acao"), acao);
    }

    public static Specification<LogAuditoria> comUsuario(Long usuarioId) {
        return (root, query, cb) -> usuarioId == null ? null : cb.equal(root.get("usuario").get("id"), usuarioId);
    }

    public static Specification<LogAuditoria> apartirDe(LocalDate dataInicio) {
        return (root, query, cb) -> dataInicio == null ? null
                : cb.greaterThanOrEqualTo(root.get("criadoEm"), dataInicio.atStartOfDay());
    }

    public static Specification<LogAuditoria> ate(LocalDate dataFim) {
        return (root, query, cb) -> dataFim == null ? null
                : cb.lessThan(root.get("criadoEm"), dataFim.plusDays(1).atStartOfDay());
    }
}
