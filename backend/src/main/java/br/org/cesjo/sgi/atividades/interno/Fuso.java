package br.org.cesjo.sgi.atividades.interno;

import java.time.Clock;
import java.time.ZoneId;

/** Fuso do Centro-Dia: "hoje" é sempre o dia em São Paulo, mesmo com o servidor em UTC. */
final class Fuso {

	static final Clock RELOGIO = Clock.system(ZoneId.of("America/Sao_Paulo"));

	private Fuso() {
	}

}
