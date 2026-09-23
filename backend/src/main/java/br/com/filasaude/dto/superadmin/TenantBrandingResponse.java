package br.com.filasaude.dto.superadmin;

import br.com.filasaude.tenancy.TenantRecord;

/**
 * Identidade visual do tenant corrente (logo e cores da Secretaria), exposta
 * SEM autenticação -- item 3.6 do levantamento de requisitos -- para que a
 * tela pública de consulta de protocolo e a tela de login apliquem o logo/cores
 * da prefeitura antes mesmo de qualquer login.
 */
public record TenantBrandingResponse(
        String nomeMunicipio,
        String corPrimaria,
        String corSecundaria,
        String logoUrl
) {
    public static TenantBrandingResponse de(TenantRecord tenant) {
        return new TenantBrandingResponse(
                tenant.nomeMunicipio(), tenant.corPrimaria(), tenant.corSecundaria(), resolverLogoUrl(tenant));
    }

    /** Tenant não encontrado ou ainda sem identidade visual customizada -- segue a paleta padrão do produto. */
    public static TenantBrandingResponse padrao() {
        return new TenantBrandingResponse(null, null, null, null);
    }

    /**
     * Um logo enviado por upload (armazenado como binário no banco master) tem prioridade
     * sobre uma eventual URL externa cadastrada -- e é servido por um caminho relativo à API
     * (o frontend resolve isso contra a URL base configurada da API).
     */
    static String resolverLogoUrl(TenantRecord tenant) {
        if (tenant.logoUpload()) {
            return "/api/public/tenant/" + tenant.slug() + "/logo";
        }
        return tenant.logoUrl();
    }
}
