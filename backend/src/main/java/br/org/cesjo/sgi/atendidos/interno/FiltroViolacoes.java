package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;

import br.org.cesjo.sgi.atendidos.TipoViolacao;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/**
 * Filtros da consulta de violações de direitos (RF009, ADR 0145). Todos opcionais. O período é pela
 * data de identificação, com as duas pontas incluídas.
 */
record FiltroViolacoes(TipoViolacao tipo, LocalDate de, LocalDate ate) {

	FiltroViolacoes {
		if (de != null && ate != null && de.isAfter(ate)) {
			throw new RegraNegocioException("O início do período não pode ser depois do fim.");
		}
	}

}
