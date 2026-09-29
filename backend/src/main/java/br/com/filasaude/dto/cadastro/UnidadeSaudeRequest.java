package br.com.filasaude.dto.cadastro;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

/**
 * Bug médio de revisão corrigido: latitude/longitude não tinham nenhuma
 * validação de faixa -- um valor digitado errado (ex.: 950 em vez de 9.50)
 * era salvo sem erro e quebrava silenciosamente o mapa/cálculo de distância
 * no frontend.
 */
public record UnidadeSaudeRequest(
        @NotBlank String nome,
        String endereco,
        @DecimalMin(value = "-90.0", message = "Latitude deve estar entre -90 e 90") @DecimalMax(value = "90.0", message = "Latitude deve estar entre -90 e 90") Double latitude,
        @DecimalMin(value = "-180.0", message = "Longitude deve estar entre -180 e 180") @DecimalMax(value = "180.0", message = "Longitude deve estar entre -180 e 180") Double longitude,
        String codigoCnes
) {
}
