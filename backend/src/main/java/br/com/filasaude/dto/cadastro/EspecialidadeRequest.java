package br.com.filasaude.dto.cadastro;

import jakarta.validation.constraints.NotBlank;

public record EspecialidadeRequest(
        @NotBlank String nome
) {
    /** Mesmo tratamento de ProcedimentoRequest: remove espaços nas pontas antes de qualquer validação/consulta. */
    public EspecialidadeRequest {
        nome = nome == null ? null : nome.strip();
    }
}
