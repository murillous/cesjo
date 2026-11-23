package br.org.cesjo.sgi.importacao.interno;

/**
 * Resultado da confirmação de uma importação (ADR 0215).
 *
 * @param importadas linhas gravadas
 * @param ignoradas linhas com erro, não gravadas
 */
record ResultadoImportacao(int importadas, int ignoradas) {
}
