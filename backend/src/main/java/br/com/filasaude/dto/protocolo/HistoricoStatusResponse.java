package br.com.filasaude.dto.protocolo;

import br.com.filasaude.domain.HistoricoStatus;

import java.time.LocalDateTime;

public record HistoricoStatusResponse(
        String statusAnterior,
        String statusNovo,
        String observacao,
        LocalDateTime criadoEm
) {
    public static HistoricoStatusResponse de(HistoricoStatus h) {
        return new HistoricoStatusResponse(
                h.getStatusAnterior() != null ? h.getStatusAnterior().name() : null,
                h.getStatusNovo().name(),
                h.getObservacao(),
                h.getCriadoEm()
        );
    }
}
