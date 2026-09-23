package br.com.filasaude.dto.cota;

import br.com.filasaude.domain.CotaAjuste;

import java.time.LocalDateTime;

public record CotaAjusteResponse(
        Integer quantidadeAnterior,
        Integer quantidadeNova,
        String usuarioNome,
        String motivo,
        LocalDateTime criadoEm
) {
    public static CotaAjusteResponse de(CotaAjuste ajuste) {
        return new CotaAjusteResponse(
                ajuste.getQuantidadeAnterior(),
                ajuste.getQuantidadeNova(),
                ajuste.getUsuario() != null ? ajuste.getUsuario().getNome() : "—",
                ajuste.getMotivo(),
                ajuste.getCriadoEm()
        );
    }
}
