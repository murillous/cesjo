package br.org.cesjo.sgi.institucional.interno;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Vínculo do colaborador como a API devolve (ADR 0170). Data e motivo de desligamento são nulos no
 * vínculo aberto.
 */
record VinculoResposta(UUID id, LocalDate dataContratacao, LocalDate dataDesligamento, String motivoDesligamento) {

	static VinculoResposta de(VinculoColaborador vinculo) {
		return new VinculoResposta(vinculo.getId(), vinculo.getDataContratacao(), vinculo.getDataDesligamento(),
				vinculo.getMotivoDesligamento());
	}

}
