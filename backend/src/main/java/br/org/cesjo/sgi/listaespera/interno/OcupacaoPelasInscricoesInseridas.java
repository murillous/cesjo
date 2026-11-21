package br.org.cesjo.sgi.listaespera.interno;

import br.org.cesjo.sgi.listaespera.OcupacaoDeVagas;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;

/**
 * Ocupação provisória: inscrições inseridas no serviço (ADR 0093). Só vale enquanto nenhum outro módulo
 * fornece {@link OcupacaoDeVagas} (ver {@link VagasConfig}).
 */
class OcupacaoPelasInscricoesInseridas implements OcupacaoDeVagas {

	private final InscricaoRepositorio inscricoes;

	OcupacaoPelasInscricoesInseridas(InscricaoRepositorio inscricoes) {
		this.inscricoes = inscricoes;
	}

	@Override
	public long vagasOcupadas() {
		return inscricoes.countBySituacao(SituacaoInscricao.INSERIDO);
	}

}
