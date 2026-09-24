package br.com.filasaude.dto.publico;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ProtocoloPublicoResponse(
        String numeroProtocolo,
        String nomePaciente,
        String nomeProcedimento,
        String nomeUnidadeSaude,
        String status,
        Integer posicaoFila,
        LocalDate dataInclusao,
        LocalDate dataPrevista,
        LocalTime horaAgendada,
        List<EtapaPublicaResponse> etapas
) {
}
