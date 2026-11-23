package br.org.cesjo.sgi.estatisticas.interno;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Uma linha do relatório de transparência (ADR 0210): um número agregado de um grupo. Diferente da
 * {@link LinhaRelatorio}, o valor pode ser uma quantidade ou uma soma em reais.
 *
 * @param grupo área, por exemplo "Doações recebidas"
 * @param indicador o que é contado ou somado, por exemplo "Alimentos"
 * @param valor o número no período; quantidade sem casas, reais com duas
 * @param unidade se o valor é quantidade ou reais
 */
record LinhaTransparencia(String grupo, String indicador, BigDecimal valor, Unidade unidade) {

	LinhaTransparencia {
		Objects.requireNonNull(valor, "valor");
		Objects.requireNonNull(unidade, "unidade");
	}

	static LinhaTransparencia quantidade(String grupo, String indicador, long quantidade) {
		return new LinhaTransparencia(grupo, indicador, BigDecimal.valueOf(quantidade), Unidade.QUANTIDADE);
	}

	static LinhaTransparencia reais(String grupo, String indicador, BigDecimal valor) {
		return new LinhaTransparencia(grupo, indicador, valor, Unidade.REAIS);
	}

	enum Unidade {

		QUANTIDADE,

		REAIS

	}

}
