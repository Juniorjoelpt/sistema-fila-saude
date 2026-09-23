package br.com.filasaude.dto.superadmin;

import jakarta.validation.constraints.NotBlank;

public record SuperadminLoginRequest(@NotBlank String email, @NotBlank String senha) {
}
