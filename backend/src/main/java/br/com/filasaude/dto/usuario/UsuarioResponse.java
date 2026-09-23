package br.com.filasaude.dto.usuario;

import br.com.filasaude.domain.Usuario;

/** Representação de um operador/regulador/ACS para telas de gestão de equipe (item 3.4). */
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String papel,
        boolean ativo
) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel().name(),
                usuario.isAtivo()
        );
    }
}
