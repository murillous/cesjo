package br.org.cesjo.sgi.listaespera.interno;

import br.org.cesjo.sgi.listaespera.OrigemEncaminhamento;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Filtros da listagem da fila (RF002). Campo nulo não filtra.
 *
 * @param nome trecho do nome da pessoa, sem diferenciar maiúsculas
 */
record FiltroFila(String nome, TipoDeficiencia tipoDeficiencia, OrigemEncaminhamento origem,
		SituacaoInscricao situacao) {

	/** Filtros que usam dado fora dos "dados básicos" do perfil Consulta (ADR 0058). */
	boolean usaDadoCompleto() {
		return tipoDeficiencia != null || origem != null;
	}

}
