package br.com.filasaude.dto.cadastro;

import br.com.filasaude.domain.enums.TipoProcedimento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProcedimentoRequest(
        @NotBlank String nome,
        @NotNull TipoProcedimento tipo,
        String especialidade
) {
    /**
     * Bug alto de revisão corrigido: o nome não era normalizado antes da
     * checagem de duplicidade (ProcedimentoController#criar usa
     * findByNomeIgnoreCase), então "Consulta Clínico Geral" e
     * "  consulta clínico geral  " (espaços extras nas pontas) eram
     * tratados como nomes diferentes e o cadastro duplicado passava sem
     * aviso. Removendo os espaços das pontas aqui, antes de qualquer
     * validação ou consulta, a checagem de duplicidade por nome passa a
     * funcionar independente de espaçamento acidental.
     */
    public ProcedimentoRequest {
        nome = nome == null ? null : nome.strip();
        especialidade = (especialidade == null || especialidade.isBlank()) ? null : especialidade.strip();
    }
}
