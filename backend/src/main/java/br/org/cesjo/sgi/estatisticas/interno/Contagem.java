package br.org.cesjo.sgi.estatisticas.interno;

/**
 * Uma barra de gráfico dos dashboards: quantos itens caem numa categoria (ADR 0195).
 *
 * @param chave código da categoria, o nome do enum, ou {@link Contagens#SEM_VALOR}
 * @param rotulo texto que a tela mostra
 * @param total quantidade
 */
record Contagem(String chave, String rotulo, long total) {
}
