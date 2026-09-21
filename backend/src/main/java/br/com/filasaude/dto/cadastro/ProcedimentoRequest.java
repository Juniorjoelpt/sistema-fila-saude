package br.com.filasaude.dto.cadastro;

import br.com.filasaude.domain.enums.TipoProcedimento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProcedimentoRequest(
        @NotBlank String nome,
        @NotNull TipoProcedimento tipo,
        String especialidade
) {
}
