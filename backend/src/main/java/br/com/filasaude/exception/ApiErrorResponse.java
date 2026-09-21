package br.com.filasaude.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        List<String> detalhes
) {
    public static ApiErrorResponse of(int status, String erro, String mensagem) {
        return new ApiErrorResponse(LocalDateTime.now(), status, erro, mensagem, List.of());
    }

    public static ApiErrorResponse of(int status, String erro, String mensagem, List<String> detalhes) {
        return new ApiErrorResponse(LocalDateTime.now(), status, erro, mensagem, detalhes);
    }
}
