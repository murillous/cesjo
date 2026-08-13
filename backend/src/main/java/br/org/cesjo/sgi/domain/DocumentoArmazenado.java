package br.org.cesjo.sgi.domain;

public record DocumentoArmazenado(
        String referencia,
        String nomeOriginal,
        String tipoConteudo,
        long tamanho
) {
}

