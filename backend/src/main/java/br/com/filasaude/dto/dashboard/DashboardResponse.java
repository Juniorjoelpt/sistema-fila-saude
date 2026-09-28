package br.com.filasaude.dto.dashboard;

import java.util.List;

public record DashboardResponse(
        long filaDeEspera,
        long agendadosNoMes,
        long realizadosNoAno,
        List<DemandaPorEspecialidade> demandaPorEspecialidade,
        Integer taxaOcupacaoGeral,
        Integer picoOcupacaoGeral,
        List<OcupacaoEspecialidade> ocupacaoPorEspecialidade,
        SlaResumo sla,
        List<TempoMedioEspecialidade> tempoMedioEsperaPorEspecialidade,
        ConfirmacaoPresencaResumo confirmacaoPresenca
) {
    public record DemandaPorEspecialidade(String especialidade, long totalAguardando) {
    }

    /**
     * Taxa de ocupação das cotas do mês corrente (item 3.3/3.4 do
     * levantamento de requisitos, Fase 2), agregada por especialidade
     * somando as cotas de todas as unidades. "Gargalo" sinaliza
     * especialidades perto ou acima de 100% da cota.
     */
    public record OcupacaoEspecialidade(
            String especialidade,
            int quantidadeTotal,
            long quantidadeUtilizada,
            int percentual,
            boolean gargalo
    ) {
    }

    /**
     * Resumo do cumprimento de SLA entre os protocolos ainda aguardando
     * atendimento -- mesmo critério (dias-limite-atrasado) usado pela
     * varredura diária de alerta (ver SlaAlertaService) e pelo badge de
     * prazo da fila. "percentualDentroPrazo" é null quando não há ninguém
     * aguardando (nada a calcular).
     */
    public record SlaResumo(
            long totalAguardando,
            long totalDentroPrazo,
            long totalAtrasado,
            Integer percentualDentroPrazo,
            int diasLimite
    ) {
    }

    /**
     * Tempo médio de espera por especialidade, em dias -- duas medidas
     * complementares: quanto tempo os protocolos AINDA aguardando já estão
     * na fila (idade do backlog atual) e quanto tempo, em média, os
     * protocolos já CONCLUÍDOS levaram da inclusão até a conclusão
     * (desfecho real). Qualquer um pode ser null quando não há protocolos
     * daquela especialidade na situação correspondente.
     */
    public record TempoMedioEspecialidade(
            String especialidade,
            Double mediaDiasEsperaAtual,
            Double mediaDiasAteConclusao
    ) {
    }

    /**
     * Taxa de confirmação de presença (melhoria pós-MVP sobre o lembrete de
     * agendamento -- ver LembreteAgendamentoService/ProtocoloConfirmacaoService):
     * entre os protocolos que já receberam o lembrete, quantos o paciente
     * confirmou, cancelou ou ainda não respondeu. "percentualConfirmacao"
     * considera só quem já respondeu (confirmou ou cancelou) -- pendentes
     * ficam de fora da taxa, mas aparecem no total à parte.
     */
    public record ConfirmacaoPresencaResumo(
            long totalLembretesEnviados,
            long totalConfirmados,
            long totalCancelados,
            long totalPendentes,
            Integer percentualConfirmacao
    ) {
    }
}
