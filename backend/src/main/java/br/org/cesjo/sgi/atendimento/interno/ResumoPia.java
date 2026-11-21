package br.org.cesjo.sgi.atendimento.interno;

import java.time.Instant;
import java.util.UUID;

/** Versão do PIA na lista da pessoa, sem o conteúdo sensível. */
record ResumoPia(UUID id, UUID pessoaId, int numeroVersao, StatusPia status, Instant criadoEm,
		Instant atualizadoEm) {

	static ResumoPia de(Pia pia) {
		return new ResumoPia(pia.getId(), pia.getPessoaId(), pia.getNumeroVersao(), pia.getStatus(),
				pia.getCriadoEm(), pia.getAtualizadoEm());
	}

}
