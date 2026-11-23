package br.org.cesjo.sgi.importacao.interno;

import java.util.List;
import java.util.function.Function;

import br.org.cesjo.sgi.importacao.interno.Previa.LinhaPrevia;

/**
 * Planilha lida e convertida linha a linha (ADR 0215). O conversor devolve o valor da linha e registra
 * nela os erros; a linha só vale se terminar sem erro. A mesma análise serve para a pré-visualização e
 * para a confirmação, que lê o arquivo de novo e grava só as linhas válidas.
 */
final class Analise<T> {

	private record Item<T>(LinhaPlanilha linha, T valor) {
	}

	private final ModeloPlanilha modelo;

	private final List<Item<T>> itens;

	private Analise(ModeloPlanilha modelo, List<Item<T>> itens) {
		this.modelo = modelo;
		this.itens = itens;
	}

	static <T> Analise<T> de(ModeloPlanilha modelo, List<LinhaPlanilha> linhas, Function<LinhaPlanilha, T> conversor) {
		return new Analise<>(modelo, linhas.stream().map(linha -> new Item<>(linha, conversor.apply(linha))).toList());
	}

	/** Valores das linhas válidas, na ordem da planilha. */
	List<T> validos() {
		return itens.stream().filter(item -> item.linha().valida()).map(Item::valor).toList();
	}

	int comErro() {
		return (int) itens.stream().filter(item -> !item.linha().valida()).count();
	}

	Previa previa() {
		List<Coluna> colunas = modelo.colunas();
		List<LinhaPrevia> linhas = itens.stream()
			.map(Item::linha)
			.map(linha -> new LinhaPrevia(linha.numero(), colunas.stream().map(linha::exibido).toList(), linha.erros()))
			.toList();
		int comErro = comErro();
		return new Previa(colunas.stream().map(Coluna::titulo).toList(), linhas, linhas.size(), linhas.size() - comErro,
				comErro);
	}

}
