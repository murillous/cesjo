package br.org.cesjo.sgi.relatorios;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Conteúdo de uma exportação: uma tabela simples com título, colunas, linhas e os filtros aplicados
 * (ADR 0041).
 *
 * <p>
 * Cada célula é {@code null} (vazia), {@link String}, {@link Number}, {@link Boolean},
 * {@link java.time.LocalDate}, {@link java.time.LocalDateTime} ou {@link java.time.Instant}. O PDF
 * formata no padrão brasileiro e a planilha guarda números e datas como valores de verdade, para a
 * pessoa poder somar e ordenar. Qualquer outro tipo sai como {@code toString()}; enums e códigos devem
 * chegar já traduzidos para o rótulo que a tela mostra.
 *
 * @param titulo título do documento e base do nome do arquivo
 * @param colunas rótulos das colunas, na ordem das células
 * @param linhas linhas da tabela, cada uma com uma célula por coluna
 * @param filtros filtros aplicados na consulta, vazio quando não há
 */
public record TabelaExportacao(String titulo, List<String> colunas, List<List<Object>> linhas,
		List<FiltroAplicado> filtros) {

	public TabelaExportacao {
		Objects.requireNonNull(titulo, "titulo");
		if (titulo.isBlank()) {
			throw new IllegalArgumentException("A exportação precisa de um título");
		}
		colunas = List.copyOf(Objects.requireNonNull(colunas, "colunas"));
		if (colunas.isEmpty()) {
			throw new IllegalArgumentException("A exportação precisa de ao menos uma coluna");
		}
		List<List<Object>> copia = new ArrayList<>();
		for (List<Object> linha : Objects.requireNonNull(linhas, "linhas")) {
			if (linha.size() != colunas.size()) {
				throw new IllegalArgumentException("A linha " + (copia.size() + 1) + " tem " + linha.size()
						+ " células, mas a tabela tem " + colunas.size() + " colunas");
			}
			// List.copyOf não aceita null, e célula vazia é null.
			copia.add(Collections.unmodifiableList(new ArrayList<>(linha)));
		}
		linhas = Collections.unmodifiableList(copia);
		filtros = filtros == null ? List.of() : List.copyOf(filtros);
	}

	/** Tabela sem filtros. */
	public TabelaExportacao(String titulo, List<String> colunas, List<List<Object>> linhas) {
		this(titulo, colunas, linhas, List.of());
	}

}
