package br.com.filasaude.dto.protocolo;

import br.com.filasaude.domain.enums.CategoriaPrioridade;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProtocoloCreateRequest(
        @NotNull Long pacienteId,
        @NotNull Long procedimentoId,
        Long unidadeSaudeId,
        @NotNull CategoriaPrioridade categoriaPrioridade,
        String processoJudicial,
        @NotNull LocalDate dataSolicitacao
) {
}
