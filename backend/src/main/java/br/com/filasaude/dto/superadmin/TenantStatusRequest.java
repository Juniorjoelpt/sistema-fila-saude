package br.com.filasaude.dto.superadmin;

import jakarta.validation.constraints.NotNull;

public record TenantStatusRequest(@NotNull Boolean ativo) {
}
