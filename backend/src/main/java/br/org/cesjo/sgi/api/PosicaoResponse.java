package br.org.cesjo.sgi.api;

public record PosicaoResponse(
        Integer entradaId,
        Integer beneficiarioId,
        String nome,
        int posicao
) {
}
