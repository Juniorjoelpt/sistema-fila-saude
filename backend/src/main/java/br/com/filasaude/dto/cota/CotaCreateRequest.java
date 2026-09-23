package br.com.filasaude.dto.cota;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;

/** Cadastro de uma nova cota (item 3.3): unidade + especialidade + mês de referência. */
public record CotaCreateRequest(
        @NotNull Long unidadeSaudeId,
        @NotBlank String especialidade,
        @NotNull YearMonth mesReferencia,
        @NotNull @Min(0) Integer quantidadeTotal
) {
}
