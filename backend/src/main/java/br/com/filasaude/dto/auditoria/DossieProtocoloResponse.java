package br.com.filasaude.dto.auditoria;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dossiê de auditoria de um protocolo para órgãos de controle (TCE/MP): dados do
 * protocolo, etapas, histórico de status e de prioridade (com quem fez e quando)
 * e a trilha de auditoria geral referente a ele. CPF/CNS saem mascarados
 * (minimização de dados, LGPD).
 */
public record DossieProtocoloResponse(
        Manifesto manifesto,
        Protocolo protocolo,
        List<Etapa> etapas,
        List<MudancaStatus> historicoStatus,
        List<MudancaPrioridade> historicoPrioridade,
        List<LogAuditoriaResponse> trilhaAuditoria
) {
    public record Manifesto(LocalDateTime geradoEm, String geradoPorEmail, String prefeitura, long totalEventos) {
    }

    public record Protocolo(
            Long id,
            String numeroProtocolo,
            String paciente,
            String cpfMascarado,
            String cnsMascarado,
            String procedimento,
            String especialidade,
            String unidadeSaude,
            String categoriaPrioridade,
            String categoriaSolicitadaPendente,
            String status,
            String processoJudicial,
            LocalDate dataSolicitacao,
            LocalDate dataInclusao,
            LocalDate dataPrevista,
            LocalDateTime criadoEm,
            LocalDateTime atualizadoEm
    ) {
    }

    public record Etapa(Integer ordem, String nome, String status, LocalDate dataRealizacao) {
    }

    public record MudancaStatus(String de, String para, String observacao, String usuario, LocalDateTime em) {
    }

    public record MudancaPrioridade(String de, String para, String motivo, String usuario, LocalDateTime em) {
    }
}
