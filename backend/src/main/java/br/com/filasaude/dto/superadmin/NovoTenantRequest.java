package br.com.filasaude.dto.superadmin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Provisionamento self-service de uma nova prefeitura (item 3.6, Fase 2).
 * O slug vira tanto o identificador do tenant (header X-Tenant-Id /
 * subdomínio) quanto a base do nome do banco físico criado para ela.
 */
public record NovoTenantRequest(
        @NotBlank String nomeMunicipio,
        @NotBlank @Pattern(
                regexp = "^[a-z0-9][a-z0-9-]{1,38}[a-z0-9]$",
                message = "Use apenas letras minúsculas, números e hífen (3 a 40 caracteres)")
        String slug,
        String corPrimaria,
        String corSecundaria,
        String logoUrl,
        @NotBlank String adminNome,
        @NotBlank @Email String adminEmail
) {
}
