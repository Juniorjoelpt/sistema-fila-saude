package br.com.filasaude.dto.protocolo;

import jakarta.validation.constraints.NotNull;

/** Agendamento de um protocolo para um horário real de agenda (com vaga controlada). */
public record AgendarHorarioRequest(
        @NotNull Long horarioAgendaId
) {
}
