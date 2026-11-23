package br.org.cesjo.sgi.importacao.interno;

import java.util.List;

/**
 * Pré-visualização de uma planilha antes da confirmação (ADR 0215): todas as linhas de dados, com os
 * valores lidos e os erros de cada uma. Nada é gravado.
 *
 * @param colunas títulos das colunas do modelo, na ordem de {@code valores}
 * @param linhas linhas de dados na ordem da planilha
 * @param total número de linhas de dados
 * @param validas linhas sem erro, que a confirmação grava
 * @param comErro linhas com erro, que a confirmação ignora
 */
record Previa(List<String> colunas, List<LinhaPrevia> linhas, int total, int validas, int comErro) {

	/**
	 * @param linha número da linha no Excel, contando o cabeçalho
	 * @param valores valor lido em cada coluna, vazio quando a célula está vazia
	 * @param erros o que corrigir; vazio quando a linha é válida
	 */
	record LinhaPrevia(int linha, List<String> valores, List<String> erros) {

		public boolean valida() {
			return erros.isEmpty();
		}

	}

}
