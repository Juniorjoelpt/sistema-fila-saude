package br.com.filasaude.dto.slaconfiguracao;

import java.util.List;

/**
 * Payload único consumido pela tela de Configurações de SLA: os limites
 * padrão do tenant (usados quando nenhum override cobre o protocolo) mais a
 * lista de overrides cadastrados. Evita duas chamadas separadas.
 */
public record SlaConfiguracoesResponse(
        int diasAtencaoPadrao,
        int diasAtrasadoPadrao,
        List<SlaConfiguracaoResponse> configuracoes
) {
}
