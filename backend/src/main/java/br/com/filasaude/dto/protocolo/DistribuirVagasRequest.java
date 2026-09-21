package br.com.filasaude.dto.protocolo;

import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

/** Ação "Distribuir Vagas" (item 3.2): agenda em lote uma lista de protocolos. */
public record DistribuirVagasRequest(
        @NotEmpty List<Long> protocoloIds,
        LocalDate dataPrevista
) {
}
