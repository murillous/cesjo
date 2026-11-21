package br.org.cesjo.sgi.atendidos;

/** Âmbito da violação, nas categorias que o RF009 divide (abuso sexual, violência física ou psicológica). */
public enum AmbitoViolacao {

	INTRAFAMILIAR("Intrafamiliar"),

	EXTRAFAMILIAR("Extrafamiliar");

	private final String rotulo;

	AmbitoViolacao(String rotulo) {
		this.rotulo = rotulo;
	}

	public String rotulo() {
		return rotulo;
	}

}
