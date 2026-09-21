package br.com.filasaude.dto.dashboard;

import java.util.List;

public record DashboardResponse(
        long filaDeEspera,
        long agendadosNoMes,
        long realizadosNoAno,
        List<DemandaPorEspecialidade> demandaPorEspecialidade
) {
    public record DemandaPorEspecialidade(String especialidade, long totalAguardando) {
    }
}
