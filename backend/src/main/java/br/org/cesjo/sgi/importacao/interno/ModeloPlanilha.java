package br.org.cesjo.sgi.importacao.interno;

import java.util.List;

/**
 * Modelo de uma planilha de importação (ADR 0215): as colunas, com título, formato e exemplo. Cada
 * importação tem o seu modelo, e ele é o único lugar que diz o que a planilha tem. O mesmo modelo gera o
 * arquivo para download, lê a planilha enviada e descreve as colunas na tela.
 */
interface ModeloPlanilha {

	/** Nome do que se importa, como "Lista de espera". */
	String titulo();

	/** Nome do arquivo do modelo para download, com a extensão {@code .xlsx}. */
	String nomeArquivo();

	List<Coluna> colunas();

}
