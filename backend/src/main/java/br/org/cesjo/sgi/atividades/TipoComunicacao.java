package br.org.cesjo.sgi.atividades;

/**
 * Tipos de contato e comunicação do RF011 (ADR 0160 e ADR 0164). O nome de cada constante é o valor
 * guardado na coluna {@code comunicacao.tipo}; mudar um nome pede migração.
 */
public enum TipoComunicacao {

	/** Contato telefônico com cuidador familiar de um usuário atendido. */
	CONTATO_CUIDADOR_FAMILIAR(false),

	/** Contato telefônico com outra instituição. */
	CONTATO_INSTITUCIONAL(false),

	/** Atendimento a público não acompanhado pelo serviço. */
	ATENDIMENTO_PUBLICO_NAO_ACOMPANHADO(false),

	/** Ofício expedido ou recebido. */
	OFICIO(true),

	/** Declaração expedida ou recebida. */
	DECLARACAO(true),

	/** Encaminhamento enviado a outra instituição ou recebido dela. */
	ENCAMINHAMENTO(true);

	private final boolean temDirecao;

	TipoComunicacao(boolean temDirecao) {
		this.temDirecao = temDirecao;
	}

	/** Se o registro diz se foi enviado ou recebido ({@link DirecaoComunicacao}). */
	public boolean temDirecao() {
		return temDirecao;
	}

}
