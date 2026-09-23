package br.com.filasaude.dto.dashboard;

import java.util.List;

public record DashboardResponse(
        long filaDeEspera,
        long agendadosNoMes,
        long realizadosNoAno,
        List<DemandaPorEspecialidade> demandaPorEspecialidade,
        Integer taxaOcupacaoGeral,
        Integer picoOcupacaoGeral,
        List<OcupacaoEspecialidade> ocupacaoPorEspecialidade
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
}
