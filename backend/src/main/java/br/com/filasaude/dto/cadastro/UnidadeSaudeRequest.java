package br.com.filasaude.dto.cadastro;

import jakarta.validation.constraints.NotBlank;

public record UnidadeSaudeRequest(
        @NotBlank String nome,
        String endereco,
        Double latitude,
        Double longitude
) {
}
