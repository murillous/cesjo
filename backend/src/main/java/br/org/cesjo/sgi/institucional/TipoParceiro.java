package br.org.cesjo.sgi.institucional;

/**
 * Tipo de instituição parceira do Centro-Dia (RF013, ADR 0172).
 *
 * <p>
 * PENDENTE-CESJO(P10): lista provisória (ADR 0172). Mudar aqui exige mudar também o
 * {@code ck_parceiro_tipo} numa migração nova e o {@code rotulos.ts} do módulo parcerias (frontend).
 */
public enum TipoParceiro {

	/** Secretarias, CRAS, CREAS, UBS e outros órgãos do poder público. */
	ORGAO_PUBLICO,

	/** Associações, fundações, igrejas e outras organizações sem fins lucrativos. */
	ORGANIZACAO_SOCIEDADE_CIVIL,

	EMPRESA,

	/** Escolas, faculdades e universidades. */
	INSTITUICAO_ENSINO,

	OUTRO

}
