package br.org.cesjo.sgi.importacao.interno;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Recusa a linha cujo nome repete uma linha anterior da planilha ou já está cadastrado (ADR 0215), para
 * que enviar a mesma planilha duas vezes não duplique os registros. O nome é comparado sem maiúsculas,
 * acentos nem espaços extras. Os cadastros são conferidos na ordem em que foram acrescentados.
 */
final class NomesRepetidos {

	private final Map<String, Integer> vistos = new HashMap<>();

	private final Map<String, String> cadastrados = new LinkedHashMap<>();

	/** Nomes já cadastrados, com a mensagem de erro para a linha que trouxer um deles. */
	NomesRepetidos cadastrados(Iterable<String> nomes, String mensagem) {
		for (String nome : nomes) {
			cadastrados.putIfAbsent(Texto.normalizar(nome), mensagem);
		}
		return this;
	}

	void conferir(LinhaPlanilha linha, Coluna colunaNome) {
		String nome = Texto.normalizar(linha.exibido(colunaNome));
		if (nome.isEmpty()) {
			return;
		}
		Integer anterior = vistos.putIfAbsent(nome, linha.numero());
		if (anterior != null) {
			linha.erro("Nome repetido: já aparece na linha " + anterior + " da planilha.");
		}
		else if (cadastrados.containsKey(nome)) {
			linha.erro(cadastrados.get(nome));
		}
	}

}
