package br.org.cesjo.sgi.api;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(
        LocalDateTime timestamp,
        int status,
        String mensagem,
        Map<String, String> campos
) {
    public static ApiError de(int status, String mensagem) {
        return new ApiError(LocalDateTime.now(), status, mensagem, Map.of());
    }
}

