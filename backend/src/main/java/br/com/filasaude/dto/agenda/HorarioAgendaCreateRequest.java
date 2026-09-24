package br.com.filasaude.dto.agenda;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Cadastro de um único horário de agenda: unidade + especialidade + data + hora + vagas. */
public record HorarioAgendaCreateRequest(
        @NotNull Long unidadeSaudeId,
        @NotBlank String especialidade,
        @NotNull LocalDate data,
        @NotNull LocalTime horaInicio,
        @NotNull @Min(1) Integer capacidadeTotal
) {
}
