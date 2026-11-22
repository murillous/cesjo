package br.org.cesjo.sgi.pessoas;

/** Escolaridade da pessoa, do cadastro completo do usuário atendido (RF007, ADR 0143). */
public enum Escolaridade {

	// PENDENTE-CESJO(P21): níveis de escolaridade provisórios (ADR 0143).

	SEM_ESCOLARIDADE("Sem escolaridade"),

	FUNDAMENTAL_INCOMPLETO("Fundamental incompleto"),

	FUNDAMENTAL_COMPLETO("Fundamental completo"),

	MEDIO_INCOMPLETO("Médio incompleto"),

	MEDIO_COMPLETO("Médio completo"),

	SUPERIOR_INCOMPLETO("Superior incompleto"),

	SUPERIOR_COMPLETO("Superior completo");

	private final String rotulo;

	Escolaridade(String rotulo) {
		this.rotulo = rotulo;
	}

	/** Nome exibido nas telas e nas exportações. */
	public String rotulo() {
		return rotulo;
	}

}
