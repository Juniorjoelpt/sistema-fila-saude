package br.com.filasaude.dto.twofactor;

import jakarta.validation.constraints.NotBlank;

/** Exige a senha de novo, para que quem pegar o celular destravado não consiga desligar o 2FA sozinho. */
public record TwoFactorDesabilitarRequest(
        @NotBlank String senha
) {
}
