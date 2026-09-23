package br.com.filasaude.dto.cota;

import br.com.filasaude.domain.Cota;

import java.time.YearMonth;

public record CotaResponse(
        Long id,
        Long unidadeSaudeId,
        String nomeUnidadeSaude,
        String especialidade,
        YearMonth mesReferencia,
        int quantidadeTotal,
        long quantidadeUtilizada,
        int percentualPreenchido
) {
    public static CotaResponse de(Cota cota, long quantidadeUtilizada) {
        int percentual = cota.getQuantidadeTotal() > 0
                ? (int) Math.round((quantidadeUtilizada * 100.0) / cota.getQuantidadeTotal())
                : 0;
        return new CotaResponse(
                cota.getId(),
                cota.getUnidadeSaude().getId(),
                cota.getUnidadeSaude().getNome(),
                cota.getEspecialidade(),
                YearMonth.from(cota.getMesReferencia()),
                cota.getQuantidadeTotal(),
                quantidadeUtilizada,
                percentual
        );
    }
}
