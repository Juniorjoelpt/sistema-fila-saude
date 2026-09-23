package br.com.filasaude.dto.usuario;

import br.com.filasaude.domain.enums.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cadastro de um novo operador/regulador/ACS (item 3.4 — Gestão de equipe). */
public record UsuarioCreateRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "A senha deve ter ao menos 6 caracteres") String senha,
        @NotNull Papel papel
) {
}
