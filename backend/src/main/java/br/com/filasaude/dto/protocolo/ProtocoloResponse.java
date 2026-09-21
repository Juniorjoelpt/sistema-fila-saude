package br.com.filasaude.dto.protocolo;

import br.com.filasaude.domain.Protocolo;

import java.time.LocalDate;

public record ProtocoloResponse(
        Long id,
        String numeroProtocolo,
        String nomePaciente,
        String nomeProcedimento,
        String nomeUnidadeSaude,
        String categoriaPrioridade,
        String status,
        String processoJudicial,
        LocalDate dataSolicitacao,
        LocalDate dataInclusao,
        LocalDate dataPrevista,
        long diasEmEspera,
        Integer posicaoFila
) {
    public static ProtocoloResponse de(Protocolo p, Integer posicaoFila) {
        return new ProtocoloResponse(
                p.getId(),
                p.getNumeroProtocolo(),
                p.getPaciente().getNome(),
                p.getProcedimento().getNome(),
                p.getUnidadeSaude() != null ? p.getUnidadeSaude().getNome() : null,
                p.getCategoriaPrioridade().name(),
                p.getStatus().name(),
                p.getProcessoJudicial(),
                p.getDataSolicitacao(),
                p.getDataInclusao(),
                p.getDataPrevista(),
                p.diasEmEspera(),
                posicaoFila
        );
    }
}
