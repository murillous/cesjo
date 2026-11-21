package br.org.cesjo.sgi.atendidos;

/** Turno em que o usuário atendido frequenta o Centro-Dia (RF007). */
public enum Turno {

	MATUTINO("Matutino"),

	VESPERTINO("Vespertino"),

	INTEGRAL("Integral");

	private final String rotulo;

	Turno(String rotulo) {
		this.rotulo = rotulo;
	}

	/** Nome exibido nas telas e nas exportações. */
	public String rotulo() {
		return rotulo;
	}

}
