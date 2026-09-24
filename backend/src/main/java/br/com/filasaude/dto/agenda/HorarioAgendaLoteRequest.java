package br.com.filasaude.dto.agenda;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Geração de horários em lote (agenda recorrente): cria um horário para cada
 * combinação de dia-da-semana dentro do período informado, do horário
 * inicial ao final, em intervalos fixos -- evita ter que cadastrar um a um
 * quando a unidade atende, por exemplo, toda segunda e quarta das 8h às 12h
 * de 20 em 20 minutos.
 */
public record HorarioAgendaLoteRequest(
        @NotNull Long unidadeSaudeId,
        @NotBlank String especialidade,
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim,
        @NotEmpty Set<DayOfWeek> diasSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim,
        @NotNull @Min(5) Integer intervaloMinutos,
        @NotNull @Min(1) Integer capacidadePorHorario
) {
}
