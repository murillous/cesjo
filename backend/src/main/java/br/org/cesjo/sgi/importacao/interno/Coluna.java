package br.org.cesjo.sgi.importacao.interno;

import java.util.List;
import java.util.Objects;

/**
 * Uma coluna do modelo de planilha (ADR 0215). O título é o texto do cabeçalho; a leitura acha a coluna
 * pelo título sem diferenciar maiúsculas, acentos nem espaços, em qualquer posição.
 *
 * @param titulo texto do cabeçalho
 * @param obrigatoria se a coluna e o valor são obrigatórios
 * @param formato explicação do que preencher, mostrada nas instruções do modelo e na tela
 * @param exemplo valor da linha de exemplo do modelo, com dados fictícios
 * @param valoresAceitos valores permitidos, vazio quando a coluna é livre
 */
record Coluna(String titulo, boolean obrigatoria, String formato, String exemplo, List<String> valoresAceitos) {

	Coluna {
		Objects.requireNonNull(titulo, "titulo");
		Objects.requireNonNull(formato, "formato");
		Objects.requireNonNull(exemplo, "exemplo");
		valoresAceitos = List.copyOf(valoresAceitos);
	}

	static Coluna obrigatoria(String titulo, String formato, String exemplo) {
		return new Coluna(titulo, true, formato, exemplo, List.of());
	}

	static Coluna opcional(String titulo, String formato, String exemplo) {
		return new Coluna(titulo, false, formato, exemplo, List.of());
	}

	static Coluna opcoes(String titulo, String exemplo, List<String> valoresAceitos) {
		return new Coluna(titulo, true, "Um dos valores aceitos.", exemplo, valoresAceitos);
	}

}
