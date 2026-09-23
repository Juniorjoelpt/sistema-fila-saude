package br.com.filasaude.dto.superadmin;

/**
 * Atualização da identidade visual (logo e cores da Secretaria) de uma
 * prefeitura já provisionada -- item 3.6 do levantamento de requisitos.
 * Campos em branco/nulos fazem o tenant voltar a usar a paleta padrão do
 * produto nessas cores específicas.
 */
public record TenantBrandingRequest(
        String corPrimaria,
        String corSecundaria,
        String logoUrl
) {
}
