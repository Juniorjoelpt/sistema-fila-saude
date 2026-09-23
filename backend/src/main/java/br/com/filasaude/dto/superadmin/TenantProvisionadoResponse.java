package br.com.filasaude.dto.superadmin;

/** Retornado uma única vez, logo após o provisionamento — contém a senha provisória do admin. */
public record TenantProvisionadoResponse(
        String slug,
        String nomeMunicipio,
        String adminEmail,
        String senhaProvisoria
) {
}
