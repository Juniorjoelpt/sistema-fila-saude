package br.com.filasaude.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Segunda etapa do login quando o usuário tem 2FA habilitado. */
public record TwoFactorLoginRequest(
        @NotBlank String loginToken,
        @NotBlank String codigo
) {
}
