package br.com.filasaude.dto.transparencia;

import br.com.filasaude.domain.TransparenciaConfig;

import java.time.LocalDateTime;

public record TransparenciaConfigResponse(
        boolean publicado,
        boolean mostrarFila,
        boolean mostrarAtendimentos,
        boolean mostrarVagas,
        boolean mostrarCriterios,
        String textoIntroducao,
        String contatoLai,
        LocalDateTime atualizadoEm
) {
    public static TransparenciaConfigResponse de(TransparenciaConfig c) {
        return new TransparenciaConfigResponse(c.isPublicado(), c.isMostrarFila(), c.isMostrarAtendimentos(),
                c.isMostrarVagas(), c.isMostrarCriterios(), c.getTextoIntroducao(), c.getContatoLai(),
                c.getAtualizadoEm());
    }
}
