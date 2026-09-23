package br.com.filasaude.dto.protocolo;

import br.com.filasaude.domain.enums.CategoriaPrioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Reclassificação manual de prioridade de um protocolo (item 3.2 / 3.5 do
 * levantamento de requisitos). O motivo é obrigatório -- é ele que sustenta
 * a auditoria contra questionamentos éticos/judiciais ao gestor.
 */
public record AlterarPrioridadeRequest(
        @NotNull CategoriaPrioridade novaCategoria,
        String processoJudicial,
        @NotBlank String motivo
) {
}
