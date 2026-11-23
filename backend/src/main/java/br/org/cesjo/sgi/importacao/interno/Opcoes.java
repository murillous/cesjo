package br.org.cesjo.sgi.importacao.interno;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Valores aceitos numa coluna de opções (ADR 0215): o rótulo exibido nas telas ou o código do enum, sem
 * diferenciar maiúsculas nem acentos. "Demanda espontânea", "demanda espontanea" e "DEMANDA_ESPONTANEA"
 * dão no mesmo valor.
 */
final class Opcoes<E extends Enum<E>> {

	private final List<String> rotulos;

	private final Map<String, E> porTexto = new HashMap<>();

	private Opcoes(Class<E> tipo, Function<E, String> rotulo) {
		E[] valores = tipo.getEnumConstants();
		this.rotulos = Arrays.stream(valores).map(rotulo).toList();
		for (E valor : valores) {
			porTexto.put(Texto.normalizar(valor.name()), valor);
			porTexto.put(Texto.normalizar(rotulo.apply(valor)), valor);
		}
	}

	static <E extends Enum<E>> Opcoes<E> de(Class<E> tipo, Function<E, String> rotulo) {
		return new Opcoes<>(tipo, rotulo);
	}

	/** Rótulos na ordem do enum, para as instruções do modelo. */
	List<String> rotulos() {
		return rotulos;
	}

	Optional<E> achar(String texto) {
		return Optional.ofNullable(porTexto.get(Texto.normalizar(texto)));
	}

}
