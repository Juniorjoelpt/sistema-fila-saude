package br.com.filasaude.dto.auth;

public record LoginResponse(
        String token,
        String nome,
        String email,
        String papel
) {
}
