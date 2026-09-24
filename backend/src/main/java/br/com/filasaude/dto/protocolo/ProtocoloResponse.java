package br.com.filasaude.dto.protocolo;

import br.com.filasaude.domain.Protocolo;

import java.time.LocalDate;
import java.time.LocalTime;

public record ProtocoloResponse(
        Long id,
        String numeroProtocolo,
        String nomePaciente,
        Long procedimentoId,
        String nomeProcedimento,
        String especialidadeProcedimento,
        Long unidadeSaudeId,
        String nomeUnidadeSaude,
        String categoriaPrioridade,
        String status,
        String processoJudicial,
        LocalDate dataSolicitacao,
        LocalDate dataInclusao,
        LocalDate dataPrevista,
        LocalTime horaAgendada,
        long diasEmEspera,
        Integer posicaoFila
) {
    public static ProtocoloResponse de(Protocolo p, Integer posicaoFila) {
        return new ProtocoloResponse(
                p.getId(),
                p.getNumeroProtocolo(),
                p.getPaciente().getNome(),
                p.getProcedimento().getId(),
                p.getProcedimento().getNome(),
                p.getProcedimento().getEspecialidade(),
                p.getUnidadeSaude() != null ? p.getUnidadeSaude().getId() : null,
                p.getUnidadeSaude() != null ? p.getUnidadeSaude().getNome() : null,
                p.getCategoriaPrioridade().name(),
                p.getStatus().name(),
                p.getProcessoJudicial(),
                p.getDataSolicitacao(),
                p.getDataInclusao(),
                p.getDataPrevista(),
                p.getHorarioAgendado() != null ? p.getHorarioAgendado().getHoraInicio() : null,
                p.diasEmEspera(),
                posicaoFila
        );
    }
}
