package br.org.cesjo.sgi.estatisticas.interno;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Período de um relatório estatístico com o nome que sai no título (ADR 0200). O relatório não sabe se
 * é mensal: um recorte novo, como o quadrimestral e o anual da issue #59, é só uma fábrica nova aqui.
 *
 * @param titulo título do relatório, por exemplo "Relatório mensal de novembro de 2026"
 * @param periodo dias que o relatório consolida
 */
record PeriodoDoRelatorio(String titulo, Periodo periodo) {

	private static final DateTimeFormatter MES_POR_EXTENSO = DateTimeFormatter.ofPattern("MMMM 'de' yyyy",
			Locale.forLanguageTag("pt-BR"));

	PeriodoDoRelatorio {
		Objects.requireNonNull(titulo, "titulo");
		Objects.requireNonNull(periodo, "periodo");
	}

	static PeriodoDoRelatorio mensal(YearMonth mes) {
		return new PeriodoDoRelatorio("Relatório mensal de " + MES_POR_EXTENSO.format(mes), Periodo.doMes(mes));
	}

}
