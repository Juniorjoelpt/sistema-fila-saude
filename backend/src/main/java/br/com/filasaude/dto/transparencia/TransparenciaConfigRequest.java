package br.com.filasaude.dto.transparencia;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransparenciaConfigRequest(
        @NotNull Boolean publicado,
        @NotNull Boolean mostrarFila,
        @NotNull Boolean mostrarAtendimentos,
        @NotNull Boolean mostrarVagas,
        @NotNull Boolean mostrarCriterios,
        @Size(max = 1000) String textoIntroducao,
        @Size(max = 300) String contatoLai
) {
}
