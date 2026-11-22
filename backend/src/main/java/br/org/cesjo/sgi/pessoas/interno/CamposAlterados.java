package br.org.cesjo.sgi.pessoas.interno;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Compara dois records do mesmo tipo e devolve os nomes, em snake_case, dos componentes que mudaram.
 * Serve à auditoria, que guarda só os nomes das colunas (ADR 0020). Os componentes têm o nome da
 * coluna em camelCase, então um campo novo no record entra na auditoria sem mudar nada aqui.
 */
final class CamposAlterados {

	private CamposAlterados() {
	}

	static <R extends Record> Set<String> entre(R antes, R depois) {
		Set<String> campos = new LinkedHashSet<>();
		for (RecordComponent componente : antes.getClass().getRecordComponents()) {
			if (!Objects.equals(valor(componente, antes), valor(componente, depois))) {
				campos.add(coluna(componente.getName()));
			}
		}
		return campos;
	}

	private static Object valor(RecordComponent componente, Record registro) {
		try {
			return componente.getAccessor().invoke(registro);
		}
		catch (IllegalAccessException | InvocationTargetException erro) {
			throw new IllegalStateException(erro);
		}
	}

	private static String coluna(String nome) {
		return nome.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT);
	}

}
