package br.com.filasaude.dto.publico;

import java.time.LocalDate;
import java.util.List;

public record ProtocoloPublicoResponse(
        String numeroProtocolo,
        String nomePaciente,
        String nomeProcedimento,
        String status,
        Integer posicaoFila,
        LocalDate dataInclusao,
        LocalDate dataPrevista,
        List<EtapaPublicaResponse> etapas
) {
}
