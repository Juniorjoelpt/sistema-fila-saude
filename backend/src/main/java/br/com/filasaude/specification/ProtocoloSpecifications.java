package br.com.filasaude.specification;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.CategoriaPrioridade;
import br.com.filasaude.domain.enums.StatusProtocolo;
import org.springframework.data.jpa.domain.Specification;

public final class ProtocoloSpecifications {

    private ProtocoloSpecifications() {
    }

    public static Specification<Protocolo> comCategoria(CategoriaPrioridade categoria) {
        return (root, query, cb) -> categoria == null ? null : cb.equal(root.get("categoriaPrioridade"), categoria);
    }

    public static Specification<Protocolo> comStatus(StatusProtocolo status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Protocolo> comProcedimento(Long procedimentoId) {
        return (root, query, cb) -> procedimentoId == null ? null
                : cb.equal(root.get("procedimento").get("id"), procedimentoId);
    }

    public static Specification<Protocolo> comAcsResponsavel(Long acsId) {
        return (root, query, cb) -> acsId == null ? null
                : cb.equal(root.get("paciente").get("acsResponsavel").get("id"), acsId);
    }
}
