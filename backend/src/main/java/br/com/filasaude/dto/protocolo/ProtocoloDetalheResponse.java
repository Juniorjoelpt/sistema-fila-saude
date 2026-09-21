package br.com.filasaude.dto.protocolo;

import java.time.LocalDate;
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
        String nomeProcedimento,
        String nomeUnidadeSaude,
        String categoriaPrioridade,
        String status,
        String processoJudicial,
        LocalDate dataSolicitacao,
        LocalDate dataInclusao,
        LocalDate dataPrevista,
        long diasEmEspera,
        Integer posicaoFila,
        List<EtapaAdminResponse> etapas,
        List<HistoricoStatusResponse> historico
) {
    public static ProtocoloDetalheResponse de(ProtocoloResponse p, List<EtapaAdminResponse> etapas,
                                               List<HistoricoStatusResponse> historico) {
        return new ProtocoloDetalheResponse(
                p.id(), p.numeroProtocolo(), p.nomePaciente(), p.nomeProcedimento(), p.nomeUnidadeSaude(),
                p.categoriaPrioridade(), p.status(), p.processoJudicial(), p.dataSolicitacao(), p.dataInclusao(),
                p.dataPrevista(), p.diasEmEspera(), p.posicaoFila(), etapas, historico
        );
    }
}
