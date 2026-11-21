package br.org.cesjo.sgi.relatorios.interno;

import java.time.ZoneId;
import java.util.Locale;

/**
 * Cores, fuso e idioma dos arquivos exportados (ADR 0040). As cores são as do guia de estilo
 * ({@code docs/frontend/guiaDeEstilo.md}), as mesmas de {@code frontend/src/styles/_tokens.scss}; o PDF
 * e a planilha usam só estas constantes.
 */
final class IdentidadeVisual {

	/** Turquesa institucional: filete do cabeçalho. Nunca em texto pequeno sobre branco. */
	static final String PRIMARIA = "#10AAA6";

	/** Texto e fundo do cabeçalho da tabela (texto branco sobre ele passa no contraste AA). */
	static final String PRIMARIA_ESCURA = "#34545C";

	/** Fundo das linhas alternadas. */
	static final String FUNDO_SECUNDARIO = "#F1F0E8";

	static final String BORDA = "#DBDDD7";

	static final String TEXTO_SECUNDARIO = "#6B6B6B";

	/** Caminho do logo no classpath, reduzido a partir de {@code docs/frontend/assets}. */
	static final String LOGO = "relatorios/logo-cesjo.png";

	/** Fuso usado para a data de geração e para os instantes nas células. */
	static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

	static final Locale IDIOMA = Locale.of("pt", "BR");

	private IdentidadeVisual() {
	}

}
