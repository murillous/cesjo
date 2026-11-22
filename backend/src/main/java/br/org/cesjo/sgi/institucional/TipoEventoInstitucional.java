package br.org.cesjo.sgi.institucional;

import java.util.Set;

/**
 * Os sete tipos de atividade institucional do RF012 (ADR 0175), com as direções que cada um aceita.
 * Mudar aqui exige mudar também o {@code ck_evento_tipo} e o {@code ck_evento_direcao} numa migração
 * nova e o {@code rotulos.ts} do módulo eventos (frontend).
 */
public enum TipoEventoInstitucional {

	REUNIAO_COORDENACAO_EQUIPE,

	FORMACAO_MULTIPROFISSIONAL,

	REUNIAO_DIRECAO_GERAL,

	MONITORAMENTO_ORGAO_GESTOR,

	/** Visita institucional realizada pelo Centro-Dia ou recebida dele. */
	VISITA_INSTITUCIONAL(DirecaoEvento.REALIZADO, DirecaoEvento.RECEBIDO),

	/** Evento realizado pelo Centro-Dia ou de que ele participou. */
	EVENTO(DirecaoEvento.REALIZADO, DirecaoEvento.PARTICIPADO),

	REUNIAO_PARCEIROS;

	private final Set<DirecaoEvento> direcoes;

	TipoEventoInstitucional(DirecaoEvento... direcoes) {
		this.direcoes = Set.of(direcoes);
	}

	/** Direções aceitas; vazio quando o tipo não tem direção. */
	public Set<DirecaoEvento> direcoes() {
		return direcoes;
	}

	/** {@code direcao} nula só vale para tipos sem direção. */
	public boolean aceita(DirecaoEvento direcao) {
		return direcao == null ? direcoes.isEmpty() : direcoes.contains(direcao);
	}

}
