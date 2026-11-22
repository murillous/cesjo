package br.org.cesjo.sgi.institucional.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.institucional.DirecaoEvento;
import br.org.cesjo.sgi.institucional.TipoEventoInstitucional;

/**
 * Reunião ou evento como a API devolve (ADR 0175).
 *
 * @param parceiro parceiro ligado, ou nulo
 */
record EventoResposta(UUID id, TipoEventoInstitucional tipo, DirecaoEvento direcao, LocalDate data, String descricao,
		String participantes, ParceiroLigado parceiro, Instant criadoEm, Instant atualizadoEm) {

	/** O bastante do parceiro para exibir e abrir a ficha. */
	record ParceiroLigado(UUID id, String nome) {
	}

	static EventoResposta de(EventoInstitucional evento, Parceiro parceiro) {
		ParceiroLigado ligado = parceiro == null ? null : new ParceiroLigado(parceiro.getId(), parceiro.getNome());
		return new EventoResposta(evento.getId(), evento.getTipo(), evento.getDirecao(), evento.getData(),
				evento.getDescricao(), evento.getParticipantes(), ligado, evento.getCriadoEm(),
				evento.getAtualizadoEm());
	}

}
