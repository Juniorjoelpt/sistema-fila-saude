package br.com.filasaude.dto.superadmin;

import br.com.filasaude.tenancy.TenantRecord;

public record TenantResumoResponse(
        Long id,
        String slug,
        String nomeMunicipio,
        boolean ativo,
        String corPrimaria,
        String corSecundaria,
        String logoUrl
) {
    public static TenantResumoResponse de(TenantRecord tenant) {
        return new TenantResumoResponse(
                tenant.id(), tenant.slug(), tenant.nomeMunicipio(), tenant.ativo(),
                tenant.corPrimaria(), tenant.corSecundaria(), TenantBrandingResponse.resolverLogoUrl(tenant));
    }
}
