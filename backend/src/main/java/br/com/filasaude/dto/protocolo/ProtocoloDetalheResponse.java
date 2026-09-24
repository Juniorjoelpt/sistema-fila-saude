package br.com.filasaude.dto.protocolo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Visão completa de um protocolo para a tela de detalhe do painel
 * administrativo: os mesmos campos do ProtocoloResponse (listagem da fila),
 * acrescidos da timeline de etapas e do histórico de mudanças de status
 * (auditoria -- item 3.5 do levantamento de requisitos).
 */
public record ProtocoloDetalheResponse(
        Long id,
        String numeroProtocolo,
        String nomePaciente,
        Long procedimentoId,
        String nomeProcedimento,
        String especialidadeProcedimento,
        Long unidadeSaudeId,
        String nomeUnidadeSaude,
        String categoriaPrioridade,
        String status,
        String processoJudicial,
        LocalDate dataSolicitacao,
        LocalDate dataInclusao,
        LocalDate dataPrevista,
        LocalTime horaAgendada,
        long diasEmEspera,
        Integer posicaoFila,
        List<EtapaAdminResponse> etapas,
        List<HistoricoStatusResponse> historico,
        List<HistoricoPrioridadeResponse> historicoPrioridade
) {
    public static ProtocoloDetalheResponse de(ProtocoloResponse p, List<EtapaAdminResponse> etapas,
                                               List<HistoricoStatusResponse> historico,
                                               List<HistoricoPrioridadeResponse> historicoPrioridade) {
        return new ProtocoloDetalheResponse(
                p.id(), p.numeroProtocolo(), p.nomePaciente(), p.procedimentoId(), p.nomeProcedimento(),
                p.especialidadeProcedimento(), p.unidadeSaudeId(), p.nomeUnidadeSaude(),
                p.categoriaPrioridade(), p.status(), p.processoJudicial(), p.dataSolicitacao(), p.dataInclusao(),
                p.dataPrevista(), p.horaAgendada(), p.diasEmEspera(), p.posicaoFila(), etapas, historico, historicoPrioridade
        );
    }
}
