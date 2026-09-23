package br.com.filasaude.dto.cota;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Ajuste em tempo real da quantidade total de uma cota (item 3.3). */
public record CotaAjusteRequest(
        @NotNull @Min(0) Integer quantidadeTotal,
        String motivo
) {
}
