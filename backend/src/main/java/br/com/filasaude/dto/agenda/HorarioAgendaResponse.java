package br.com.filasaude.dto.agenda;

import br.com.filasaude.domain.HorarioAgenda;

import java.time.LocalDate;
import java.time.LocalTime;

public record HorarioAgendaResponse(
        Long id,
        Long unidadeSaudeId,
        String nomeUnidadeSaude,
        String especialidade,
        LocalDate data,
        LocalTime horaInicio,
        int capacidadeTotal,
        long vagasOcupadas,
        int vagasDisponiveis
) {
    public static HorarioAgendaResponse de(HorarioAgenda h, long vagasOcupadas) {
        int disponiveis = (int) Math.max(0, h.getCapacidadeTotal() - vagasOcupadas);
        return new HorarioAgendaResponse(
                h.getId(),
                h.getUnidadeSaude().getId(),
                h.getUnidadeSaude().getNome(),
                h.getEspecialidade(),
                h.getData(),
                h.getHoraInicio(),
                h.getCapacidadeTotal(),
                vagasOcupadas,
                disponiveis
        );
    }
}
