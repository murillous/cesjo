package br.org.cesjo.sgi.estatisticas.interno;

import java.time.ZoneId;

/** Fuso do Centro-Dia, que decide o "hoje" da faixa etária e os limites dos períodos. */
final class Fuso {

	static final ZoneId CENTRO_DIA = ZoneId.of("America/Sao_Paulo");

	private Fuso() {
	}

}
