package br.org.cesjo.sgi.atividades;

/**
 * Tipos de contato e comunicação do RF011 (ADR 0160). O nome de cada constante é o valor guardado na
 * coluna {@code comunicacao.tipo}; mudar um nome pede migração.
 */
public enum TipoComunicacao {

	/** Contato telefônico com cuidador familiar de um usuário atendido. */
	CONTATO_CUIDADOR_FAMILIAR,

	/** Contato telefônico com outra instituição. */
	CONTATO_INSTITUCIONAL,

	/** Atendimento a público não acompanhado pelo serviço. */
	ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO

}
