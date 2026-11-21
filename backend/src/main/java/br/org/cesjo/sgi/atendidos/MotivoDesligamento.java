package br.org.cesjo.sgi.atendidos;

/** Motivos de desligamento do serviço (RF008). */
public enum MotivoDesligamento {

	EVASAO("Evasão"),

	MUDANCA_MUNICIPIO("Mudança para outro município"),

	SOLICITACAO_FAMILIA("Solicitação da família"),

	ALCANCE_OBJETIVOS("Alcance dos objetivos"),

	/** Exige observação explicando o motivo (ADR 0110). */
	OUTROS("Outros");

	private final String rotulo;

	MotivoDesligamento(String rotulo) {
		this.rotulo = rotulo;
	}

	/** Nome exibido nas telas e nas exportações. */
	public String rotulo() {
		return rotulo;
	}

}
