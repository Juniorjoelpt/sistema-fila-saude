package br.com.filasaude.dto.protocolo;

import br.com.filasaude.domain.EtapaProtocolo;

import java.time.LocalDate;

/**
 * Igual ao EtapaPublicaResponse, mas com o id -- necessário para o painel
 * administrativo poder marcar uma etapa específica (PATCH .../etapas/{id}).
 */
public record EtapaAdminResponse(
        Long id,
        String nomeEtapa,
        Integer ordem,
        String status,
        LocalDate dataRealizacao
) {
    public static EtapaAdminResponse de(EtapaProtocolo etapa) {
        return new EtapaAdminResponse(
                etapa.getId(),
                etapa.getNomeEtapa(),
                etapa.getOrdem(),
                etapa.getStatus().name(),
                etapa.getDataRealizacao()
        );
    }
}
