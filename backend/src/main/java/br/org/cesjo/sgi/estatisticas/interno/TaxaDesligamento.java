package br.org.cesjo.sgi.estatisticas.interno;

/**
 * Desligamentos de um motivo no período e a taxa sobre os usuários atendidos no período (ADR 0198).
 *
 * @param chave código do motivo
 * @param rotulo texto que a tela mostra
 * @param total desligamentos do motivo no período
 * @param taxa {@code total} dividido pelos usuários atendidos no período, de 0 a 1; zero quando ninguém
 * foi atendido
 */
record TaxaDesligamento(String chave, String rotulo, long total, double taxa) {
}
