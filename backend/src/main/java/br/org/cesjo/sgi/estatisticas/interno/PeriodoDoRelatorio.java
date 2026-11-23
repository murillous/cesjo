package br.org.cesjo.sgi.estatisticas.interno;

import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Período de um relatório estatístico com o nome que sai no título (ADR 0200). O relatório não sabe se
 * é mensal, quadrimestral ou anual: cada recorte é só uma fábrica aqui (ADR 0203).
 *
 * @param titulo título do relatório, por exemplo "Relatório mensal de novembro de 2026"
 * @param periodo dias que o relatório consolida
 */
record PeriodoDoRelatorio(String titulo, Periodo periodo) {

	/** Quadrimestres de um ano: 1 (janeiro a abril), 2 (maio a agosto) e 3 (setembro a dezembro). */
	static final int QUADRIMESTRES_NO_ANO = 3;

	private static final DateTimeFormatter NOME_DO_MES = DateTimeFormatter.ofPattern("MMMM",
			Locale.forLanguageTag("pt-BR"));

	private static final DateTimeFormatter MES_POR_EXTENSO = DateTimeFormatter.ofPattern("MMMM 'de' yyyy",
			Locale.forLanguageTag("pt-BR"));

	PeriodoDoRelatorio {
		Objects.requireNonNull(titulo, "titulo");
		Objects.requireNonNull(periodo, "periodo");
	}

	static PeriodoDoRelatorio mensal(YearMonth mes) {
		return new PeriodoDoRelatorio("Relatório mensal de " + MES_POR_EXTENSO.format(mes), Periodo.doMes(mes));
	}

	/**
	 * O quadrimestre do ano civil: 1 é janeiro a abril, 2 é maio a agosto e 3 é setembro a dezembro.
	 * PENDENTE-CESJO(P9): divisão provisória do ano em quadrimestres (ADR 0203).
	 */
	static PeriodoDoRelatorio quadrimestral(Year ano, int quadrimestre) {
		if (quadrimestre < 1 || quadrimestre > QUADRIMESTRES_NO_ANO) {
			throw new IllegalArgumentException("quadrimestre fora de 1 a 3: " + quadrimestre);
		}
		YearMonth primeiro = ano.atMonth(Month.of((quadrimestre - 1) * 4 + 1));
		YearMonth ultimo = primeiro.plusMonths(3);
		String titulo = "Relatório quadrimestral de " + NOME_DO_MES.format(primeiro) + " a "
				+ NOME_DO_MES.format(ultimo) + " de " + ano;
		return new PeriodoDoRelatorio(titulo, new Periodo(primeiro.atDay(1), ultimo.atEndOfMonth()));
	}

	/** O ano civil inteiro, de 1º de janeiro a 31 de dezembro. */
	static PeriodoDoRelatorio anual(Year ano) {
		Periodo periodo = new Periodo(ano.atDay(1), ano.atMonth(Month.DECEMBER).atEndOfMonth());
		return new PeriodoDoRelatorio("Relatório anual de " + ano, periodo);
	}

}
