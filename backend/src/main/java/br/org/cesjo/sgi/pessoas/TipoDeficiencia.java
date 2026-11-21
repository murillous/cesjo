package br.org.cesjo.sgi.pessoas;

/**
 * Tipos de deficiência atendidos pelo Centro-Dia (documento de requisitos, seção 1, e RF001). Uma
 * pessoa tem um tipo; quem tem mais de uma deficiência é cadastrado como {@link #MULTIPLA}.
 */
public enum TipoDeficiencia {

	FISICA("Física"),

	SENSORIAL("Sensorial"),

	INTELECTUAL("Intelectual"),

	MULTIPLA("Múltipla"),

	TEA("TEA"),

	T21("Síndrome de Down (T21)");

	private final String rotulo;

	TipoDeficiencia(String rotulo) {
		this.rotulo = rotulo;
	}

	/** Nome exibido nas telas e nas exportações. */
	public String rotulo() {
		return rotulo;
	}

}
