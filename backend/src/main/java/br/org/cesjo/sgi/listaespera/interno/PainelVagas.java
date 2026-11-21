package br.org.cesjo.sgi.listaespera.interno;

import java.time.Instant;

/**
 * Vagas do serviço e tamanho da fila (RF003, ADR 0092).
 *
 * @param capacidade vagas totais; nula enquanto o Coordenador não definir
 * @param ocupadas vagas ocupadas, pela {@link br.org.cesjo.sgi.listaespera.OcupacaoDeVagas}
 * @param disponiveis capacidade menos ocupadas, nunca negativo; zero sem capacidade definida
 * @param tamanhoFila inscrições aguardando
 * @param definidaEm quando a capacidade vigente foi definida
 */
record PainelVagas(Integer capacidade, long ocupadas, long disponiveis, long tamanhoFila, Instant definidaEm) {
}
