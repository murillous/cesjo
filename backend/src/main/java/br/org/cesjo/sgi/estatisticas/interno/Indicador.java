package br.org.cesjo.sgi.estatisticas.interno;

import java.util.List;

/** Um item do relatório estatístico: calcula uma ou mais linhas para um período (ADR 0200). */
@FunctionalInterface
interface Indicador {

	List<LinhaRelatorio> calcular(Periodo periodo);

}
