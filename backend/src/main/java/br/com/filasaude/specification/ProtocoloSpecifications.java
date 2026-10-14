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

    /**
     * Busca livre da Fila: nome do paciente, número do protocolo ou CPF/CNS
     * (aceita com ou sem pontuação -- só os dígitos digitados são comparados).
     */
    public static Specification<Protocolo> comBusca(String busca) {
        return (root, query, cb) -> {
            if (busca == null || busca.isBlank()) {
                return null;
            }
            String termo = "%" + busca.trim().toLowerCase().replace("%", "").replace("_", "") + "%";
            var paciente = root.get("paciente");
            var predicados = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicados.add(cb.like(cb.lower(paciente.get("nome")), termo));
            predicados.add(cb.like(cb.lower(root.get("numeroProtocolo")), termo));
            String digitos = busca.replaceAll("\\D", "");
            if (!digitos.isEmpty()) {
                predicados.add(cb.like(paciente.get("cpf"), "%" + digitos + "%"));
                predicados.add(cb.like(paciente.get("cns"), "%" + digitos + "%"));
            }
            return cb.or(predicados.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    public static Specification<Protocolo> comAcsResponsavel(Long acsId) {
        return (root, query, cb) -> acsId == null ? null
                : cb.equal(root.get("paciente").get("acsResponsavel").get("id"), acsId);
    }
}
