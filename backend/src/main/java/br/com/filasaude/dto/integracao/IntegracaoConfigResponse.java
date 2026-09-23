package br.com.filasaude.dto.integracao;

import br.com.filasaude.domain.IntegracaoConfig;
import br.com.filasaude.domain.enums.TipoIntegracao;

import java.time.LocalDateTime;

/**
 * O token nunca é devolvido em texto puro -- só "tokenConfigurado" indica se
 * uma credencial já foi salva, para a tela mostrar "configurado" sem expor
 * o valor.
 */
public record IntegracaoConfigResponse(
        TipoIntegracao tipo,
        String baseUrl,
        boolean tokenConfigurado,
        boolean ativo,
        LocalDateTime atualizadoEm,
        String atualizadoPorNome
) {
    public static IntegracaoConfigResponse de(TipoIntegracao tipo, IntegracaoConfig config) {
        if (config == null) {
            return new IntegracaoConfigResponse(tipo, null, false, false, null, null);
        }
        return new IntegracaoConfigResponse(
                tipo,
                config.getBaseUrl(),
                config.getToken() != null && !config.getToken().isBlank(),
                config.isAtivo(),
                config.getAtualizadoEm(),
                config.getAtualizadoPor() != null ? config.getAtualizadoPor().getNome() : null
        );
    }
}
