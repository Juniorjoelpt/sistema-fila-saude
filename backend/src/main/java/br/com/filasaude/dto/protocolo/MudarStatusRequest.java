package br.com.filasaude.dto.protocolo;

import br.com.filasaude.domain.enums.StatusProtocolo;
import jakarta.validation.constraints.NotNull;

public record MudarStatusRequest(
        @NotNull StatusProtocolo novoStatus,
        String observacao
) {
}
