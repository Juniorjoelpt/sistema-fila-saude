package br.com.filasaude.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Troca da própria senha (ver ContaService#alterarSenha). */
public record AlterarSenhaRequest(
        @NotBlank String senhaAtual,
        @NotBlank @Size(min = 8, message = "A nova senha deve ter ao menos 8 caracteres") String novaSenha
) {
}
