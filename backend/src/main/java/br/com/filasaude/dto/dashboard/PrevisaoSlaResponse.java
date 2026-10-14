package br.com.filasaude.dto.dashboard;

import java.util.List;

/**
 * Dashboard preditivo de SLA: além de mostrar quem JÁ está atrasado, estima
 * quem VAI estourar o prazo se o ritmo de atendimento recente se mantiver.
 *
 * Método (propositalmente simples e explicável a um gestor): para cada
 * procedimento, a vazão é a média diária de protocolos que saíram da fila
 * (AGUARDANDO -> AGENDADO/EM_ANDAMENTO/CONCLUIDO) nos últimos
 * {@code janelaDias} dias. Um protocolo na posição N da fila do seu
 * procedimento deve ser atendido em cerca de N / vazão dias; se os dias já
 * esperados somados a essa estimativa passam do limite de SLA dele, ele é
 * classificado como "vai atrasar". Sem nenhuma saída na janela não há base
 * para prever (SEM_HISTORICO) -- o sistema não inventa um número.
 */
public record PrevisaoSlaResponse(
        int janelaDias,
        Resumo resumo,
        List<PrevisaoProcedimento> porProcedimento,
        List<ProtocoloEmRisco> protocolosEmRisco
) {
    public record Resumo(
            long totalAguardando,
            long jaAtrasados,
            long vaoAtrasar,
            long semHistorico,
            /** % dos protocolos (com base para previsão) que terminam dentro do prazo; null se não houver base. */
            Integer percentualPrevistoNoPrazo
    ) {
    }

    public record PrevisaoProcedimento(
            Long procedimentoId,
            String procedimento,
            String especialidade,
            long aguardando,
            long entradasNaJanela,
            long saidasNaJanela,
            /** Protocolos atendidos por semana, em média, na janela. */
            double vazaoSemanal,
            /** Dias para esvaziar a fila atual no ritmo recente; null sem histórico. */
            Integer diasParaEsvaziarFila,
            long jaAtrasados,
            long vaoAtrasar,
            /** CRESCENDO (entra mais do que sai), ESTAVEL, DIMINUINDO ou SEM_DADOS. */
            String tendencia
    ) {
    }

    public record ProtocoloEmRisco(
            Long protocoloId,
            String numeroProtocolo,
            String nomePaciente,
            String procedimento,
            String categoriaPrioridade,
            int posicaoFila,
            long diasEmEspera,
            int limiteDias,
            /** Em quantos dias o protocolo passa a contar como "Atrasado" se nada mudar. */
            long diasAteAtraso,
            /** Dias estimados até ser atendido, no ritmo recente. */
            int atendimentoEstimadoEmDias
    ) {
    }
}
