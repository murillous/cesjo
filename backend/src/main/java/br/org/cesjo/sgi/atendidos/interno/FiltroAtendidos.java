package br.org.cesjo.sgi.atendidos.interno;

import br.org.cesjo.sgi.atendidos.FaixaEtaria;
import br.org.cesjo.sgi.atendidos.SituacaoInsercao;
import br.org.cesjo.sgi.atendidos.Turno;
import br.org.cesjo.sgi.pessoas.Genero;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * Filtros da listagem de usuários atendidos (ADR 0083). Todos opcionais. Gênero e bairro vêm do
 * cadastro completo (ADR 0140).
 *
 * @param nome parte do nome, sem diferenciar maiúsculas
 * @param bairro parte do nome do bairro, sem diferenciar maiúsculas
 */
record FiltroAtendidos(String nome, Turno turno, SituacaoInsercao situacao, TipoDeficiencia tipoDeficiencia,
		FaixaEtaria faixaEtaria, Genero genero, String bairro) {

	// PENDENTE-CESJO(P8): tipo de deficiência, faixa etária, gênero e bairro não são dados básicos (ADR 0083 e
	// ADR 0140).
	/** Filtros que revelam dado além dos básicos e exigem a ficha completa (ADR 0082). */
	boolean usaDadoCompleto() {
		return tipoDeficiencia != null || faixaEtaria != null || genero != null || (bairro != null && !bairro.isBlank());
	}

}
