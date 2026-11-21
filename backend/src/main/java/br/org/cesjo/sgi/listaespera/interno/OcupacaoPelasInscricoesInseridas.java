package br.org.cesjo.sgi.listaespera.interno;

import org.springframework.stereotype.Component;

import br.org.cesjo.sgi.listaespera.OcupacaoDeVagas;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;

/**
 * Ocupação provisória: inscrições inseridas no serviço (ADR 0093). Sai quando o módulo de atendidos
 * entrar com a contagem das inserções ativas.
 */
@Component
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
