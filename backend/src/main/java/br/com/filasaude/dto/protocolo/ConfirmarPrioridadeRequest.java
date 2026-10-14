package br.com.filasaude.dto.protocolo;

import jakarta.validation.constraints.NotNull;

/**
 * Decisão do regulador sobre uma Urgência/Judicial solicitada por um ACS.
 * O motivo é obrigatório ao recusar (fica no histórico de prioridade).
 */
public record ConfirmarPrioridadeRequest(
        @NotNull Boolean confirmar,
        String motivo
) {
}
