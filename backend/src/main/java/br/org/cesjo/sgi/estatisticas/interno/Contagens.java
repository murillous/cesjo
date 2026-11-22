package br.org.cesjo.sgi.estatisticas.interno;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Conta itens por categoria para os gráficos dos dashboards (ADR 0195). */
final class Contagens {

	/** Chave da barra dos itens sem categoria, como gênero não informado. */
	static final String SEM_VALOR = "SEM_VALOR";

	private Contagens() {
	}

	/** Uma contagem por valor do enum, na ordem do enum, a partir de um mapa já contado. */
	static <E extends Enum<E>> List<Contagem> doMapa(Class<E> tipo, Function<E, String> rotulo, Map<E, Long> totais) {
		List<Contagem> contagens = new ArrayList<>();
		for (E valor : tipo.getEnumConstants()) {
			contagens.add(new Contagem(valor.name(), rotulo.apply(valor), totais.getOrDefault(valor, 0L)));
		}
		return List.copyOf(contagens);
	}

	/**
	 * Uma contagem por valor do enum, na ordem do enum, inclusive os que têm zero, para o gráfico ter
	 * sempre as mesmas barras. Os itens sem categoria ({@code null}) vão numa barra a mais, no fim, só
	 * quando houver algum.
	 */
	static <E extends Enum<E>> List<Contagem> porCategoria(Class<E> tipo, Function<E, String> rotulo,
			Collection<E> itens, String rotuloSemValor) {
		Map<E, Long> totais = new EnumMap<>(tipo);
		long semValor = 0;
		for (E item : itens) {
			if (item == null) {
				semValor++;
			}
			else {
				totais.merge(item, 1L, Long::sum);
			}
		}
		List<Contagem> contagens = new ArrayList<>();
		for (E valor : tipo.getEnumConstants()) {
			contagens.add(new Contagem(valor.name(), rotulo.apply(valor), totais.getOrDefault(valor, 0L)));
		}
		if (semValor > 0) {
			contagens.add(new Contagem(SEM_VALOR, rotuloSemValor, semValor));
		}
		return List.copyOf(contagens);
	}

}
