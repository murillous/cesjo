package br.org.cesjo.sgi.atendidos.interno;

import br.org.cesjo.sgi.atendidos.FaixaEtaria;
import br.org.cesjo.sgi.atendidos.SituacaoInsercao;
import br.org.cesjo.sgi.atendidos.Turno;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Filtros da listagem de usuários atendidos (ADR 0083). Todos opcionais. Gênero e bairro entram com o
 * cadastro completo (#40), quando a pessoa tiver esses campos.
 *
 * @param nome parte do nome, sem diferenciar maiúsculas
 */
record FiltroAtendidos(String nome, Turno turno, SituacaoInsercao situacao, TipoDeficiencia tipoDeficiencia,
		FaixaEtaria faixaEtaria) {

	// PENDENTE-CESJO(P8): tipo de deficiência e faixa etária não são dados básicos (ADR 0083).
	/** Filtros que revelam dado além dos básicos e exigem a ficha completa (ADR 0082). */
	boolean usaDadoCompleto() {
		return tipoDeficiencia != null || faixaEtaria != null;
	}

}
