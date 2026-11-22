package br.org.cesjo.sgi.atividades;

/**
 * Tipos de atividade do RF010 (ADR 0150). O nome de cada constante é o valor guardado na coluna
 * {@code atividade.tipo}; mudar um nome pede migração.
 */
public enum TipoAtividade {

	/** Atividade com as famílias dos usuários atendidos. */
	COM_FAMILIAS,

	/** Atividade com os usuários atendidos. */
	COM_USUARIOS,

	/** Atividade que envolve os usuários atendidos e as suas famílias. */
	COM_USUARIOS_E_FAMILIAS

}
