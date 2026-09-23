package br.com.filasaude.dto.twofactor;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorConfirmarRequest(
        @NotBlank String codigo
) {
}
