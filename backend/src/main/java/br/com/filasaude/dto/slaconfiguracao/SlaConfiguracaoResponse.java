package br.com.filasaude.dto.slaconfiguracao;

import br.com.filasaude.domain.SlaConfiguracao;

import java.time.LocalDateTime;

public record SlaConfiguracaoResponse(
        Long id,
        String especialidade,
        Long procedimentoId,
        String nomeProcedimento,
        int diasAtencao,
        int diasAtrasado,
        LocalDateTime atualizadoEm
) {
    public static SlaConfiguracaoResponse de(SlaConfiguracao config) {
        return new SlaConfiguracaoResponse(
                config.getId(),
                config.getEspecialidade(),
                config.getProcedimento() != null ? config.getProcedimento().getId() : null,
                config.getProcedimento() != null ? config.getProcedimento().getNome() : null,
                config.getDiasAtencao(),
                config.getDiasAtrasado(),
                config.getAtualizadoEm()
        );
    }
}
