package br.org.cesjo.sgi.api;

import java.time.LocalDate;

public record ListaEsperaItemResponse(
        Integer entradaId,
        Integer beneficiarioId,
        int posicao,
        String nome,
        String tipoDeficiencia,
        LocalDate dataEntrada,
        boolean beneficiarioBpc,
        boolean inscritoCadunico,
        String origemEncaminhamento
) {
}
