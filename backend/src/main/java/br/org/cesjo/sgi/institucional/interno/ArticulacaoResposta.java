package br.org.cesjo.sgi.institucional.interno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Ação de articulação como a API devolve (ADR 0172). */
record ArticulacaoResposta(UUID id, LocalDate data, String descricao, Instant criadoEm) {

	static ArticulacaoResposta de(ArticulacaoParceria articulacao) {
		return new ArticulacaoResposta(articulacao.getId(), articulacao.getData(), articulacao.getDescricao(),
				articulacao.getCriadoEm());
	}

}
