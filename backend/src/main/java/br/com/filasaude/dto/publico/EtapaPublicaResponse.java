package br.com.filasaude.dto.publico;

import br.com.filasaude.domain.EtapaProtocolo;

import java.time.LocalDate;

public record EtapaPublicaResponse(
        String nomeEtapa,
        Integer ordem,
        String status,
        LocalDate dataRealizacao
) {
    public static EtapaPublicaResponse de(EtapaProtocolo etapa) {
        return new EtapaPublicaResponse(
                etapa.getNomeEtapa(),
                etapa.getOrdem(),
                etapa.getStatus().name(),
                etapa.getDataRealizacao()
        );
    }
}
