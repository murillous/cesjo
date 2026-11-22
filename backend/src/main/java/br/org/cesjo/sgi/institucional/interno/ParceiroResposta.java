package br.org.cesjo.sgi.institucional.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.institucional.TipoParceiro;

/** Parceiro como a API devolve (ADR 0172). */
record ParceiroResposta(UUID id, String nome, TipoParceiro tipo, String instrumento, LocalDate vigenciaInicio,
		LocalDate vigenciaFim, String contato, String observacoes, boolean ativo, Instant criadoEm,
		Instant atualizadoEm) {

	static ParceiroResposta de(Parceiro parceiro) {
		return new ParceiroResposta(parceiro.getId(), parceiro.getNome(), parceiro.getTipo(),
				parceiro.getInstrumento(), parceiro.getVigenciaInicio(), parceiro.getVigenciaFim(),
				parceiro.getContato(), parceiro.getObservacoes(), parceiro.isAtivo(), parceiro.getCriadoEm(),
				parceiro.getAtualizadoEm());
	}

}
