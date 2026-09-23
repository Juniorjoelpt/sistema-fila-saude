package br.com.filasaude.dto.integracao;

/**
 * token nulo/em branco mantém o token já salvo (permite ativar/editar a URL
 * sem reenviar a credencial); string não vazia substitui o token salvo.
 */
public record IntegracaoConfigRequest(
        String baseUrl,
        String token,
        boolean ativo
) {
}
