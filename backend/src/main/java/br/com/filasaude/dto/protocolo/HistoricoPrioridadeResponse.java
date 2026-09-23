package br.com.filasaude.dto.protocolo;

import br.com.filasaude.domain.HistoricoPrioridade;

import java.time.LocalDateTime;

public record HistoricoPrioridadeResponse(
        String prioridadeAnterior,
        String prioridadeNova,
        String usuarioNome,
        String motivo,
        LocalDateTime criadoEm
) {
    public static HistoricoPrioridadeResponse de(HistoricoPrioridade h) {
        return new HistoricoPrioridadeResponse(
                h.getPrioridadeAnterior().name(),
                h.getPrioridadeNova().name(),
                h.getUsuario() != null ? h.getUsuario().getNome() : null,
                h.getMotivo(),
                h.getCriadoEm()
        );
    }
}
