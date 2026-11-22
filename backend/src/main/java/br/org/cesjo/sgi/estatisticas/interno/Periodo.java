package br.org.cesjo.sgi.estatisticas.interno;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/**
 * Intervalo de datas dos dashboards e relatórios, com início e fim inclusive (ADR 0198 e ADR 0199).
 * Os relatórios por mês, quadrimestre e ano são só jeitos de montar um período.
 */
record Periodo(LocalDate inicio, LocalDate fim) {

	Periodo {
		Objects.requireNonNull(inicio, "inicio");
		Objects.requireNonNull(fim, "fim");
		if (fim.isBefore(inicio)) {
			throw new RegraNegocioException("A data final do período não pode ser anterior à inicial.");
		}
	}

	/** O mês inteiro, do dia 1 ao último dia. */
	static Periodo doMes(YearMonth mes) {
		return new Periodo(mes.atDay(1), mes.atEndOfMonth());
	}

}
