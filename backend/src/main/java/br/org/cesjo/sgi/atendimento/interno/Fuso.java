package br.org.cesjo.sgi.atendimento.interno;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Fuso do Centro-Dia. A data e a hora das visitas são locais e ficam sem fuso no banco (ADR 0070); "hoje"
 * é sempre o dia em São Paulo, mesmo com o servidor em UTC.
 */
final class Fuso {

	static final ZoneId CENTRO_DIA = ZoneId.of("America/Sao_Paulo");

	static final Clock RELOGIO = Clock.system(CENTRO_DIA);

	private Fuso() {
	}

}
