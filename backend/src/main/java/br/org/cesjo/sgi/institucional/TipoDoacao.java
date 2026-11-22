package br.org.cesjo.sgi.institucional;

/**
 * O que foi doado (RF015, ADR 0178).
 *
 * <p>
 * PENDENTE-CESJO(P11): lista provisória (ADR 0178). Mudar aqui exige mudar também o
 * {@code ck_doacao_tipo} numa migração nova e o {@code rotulos.ts} do módulo doações (frontend).
 */
public enum TipoDoacao {

	/** Única que aceita valor em reais. */
	DINHEIRO,

	ALIMENTOS,

	VESTUARIO,

	/** Produtos de higiene pessoal e de limpeza. */
	HIGIENE_LIMPEZA,

	/** Material pedagógico, de artesanato e de oficinas. */
	MATERIAL_PEDAGOGICO,

	MOVEIS_EQUIPAMENTOS,

	OUTRO

}
