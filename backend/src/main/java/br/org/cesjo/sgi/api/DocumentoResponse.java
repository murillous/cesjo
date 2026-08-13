package br.org.cesjo.sgi.api;

public record DocumentoResponse(
        String nomeOriginal,
        String tipoConteudo,
        long tamanho
) {
}

