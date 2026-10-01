package br.com.filasaude.dto.slaconfiguracao;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cadastro/edição de um override de SLA. Exatamente um de
 * {@code especialidade}/{@code procedimentoId} deve vir preenchido -- a
 * validação completa (exclusividade, duplicidade, dias_atencao &lt;
 * dias_atrasado) fica em {@code SlaConfiguracaoService}, não em anotações
 * de bean validation, porque depende de regra entre campos.
 */
public record SlaConfiguracaoRequest(
        String especialidade,
        Long procedimentoId,
        @NotNull @Min(1) Integer diasAtencao,
        @NotNull @Min(1) Integer diasAtrasado
) {
}
