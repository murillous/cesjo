package br.org.cesjo.sgi.estatisticas.interno;

/**
 * Uma linha do relatório estatístico: um quantitativo de um grupo (ADR 0200).
 *
 * @param grupo área do serviço, por exemplo "Lista de espera"
 * @param indicador o que é contado, por exemplo "Novas inscrições"
 * @param quantidade o número no período
 */
record LinhaRelatorio(String grupo, String indicador, long quantidade) {
}
