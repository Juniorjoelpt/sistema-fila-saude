package br.com.filasaude.dto.usuario;

import br.com.filasaude.domain.enums.Papel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Atualização de um operador existente: nome, papel e situação (ativo/inativo).
 * A senha é opcional — quando ausente (null ou em branco), a senha atual é mantida.
 */
public record UsuarioUpdateRequest(
        @NotBlank String nome,
        @NotNull Papel papel,
        boolean ativo,
        String novaSenha
) {
}
