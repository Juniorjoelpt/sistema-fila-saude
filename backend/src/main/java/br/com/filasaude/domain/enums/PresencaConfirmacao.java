package br.com.filasaude.domain.enums;

/**
 * Resposta do paciente ao lembrete de agendamento (ver LembreteAgendamentoService),
 * dada por ele mesmo, sem login, pelo link de confirmacao enviado por e-mail.
 * NULL no protocolo até que um lembrete tenha sido enviado.
 */
public enum PresencaConfirmacao {
    PENDENTE,
    CONFIRMADA,
    CANCELADA
}
