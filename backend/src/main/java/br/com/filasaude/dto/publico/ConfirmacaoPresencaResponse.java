package br.com.filasaude.dto.publico;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Dados exibidos na tela pública de confirmação de presença (ver
 * ProtocoloConfirmacaoController) -- o mínimo necessário para o paciente
 * reconhecer o próprio agendamento antes de confirmar ou cancelar, sem
 * expor o restante do protocolo.
 */
public record ConfirmacaoPresencaResponse(
        String numeroProtocolo,
        String nomePaciente,
        String nomeProcedimento,
        String nomeUnidadeSaude,
        LocalDate dataPrevista,
        LocalTime horaAgendada,
        String presencaConfirmacao
) {
}
