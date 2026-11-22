package br.org.cesjo.sgi.pessoas;

/**
 * Crença religiosa, com as opções do RF007. Sem valor quer dizer "não informada". É dado sensível: só
 * quem tem permissão de dados sensíveis lê e edita, e a leitura é auditada (ADR 0141 e ADR 0142).
 */
public enum CrencaReligiosa {

	CATOLICA("Católica"),

	EVANGELICA("Evangélica"),

	TESTEMUNHA_DE_JEOVA("Testemunha de Jeová"),

	MATRIZ_AFRICANA("Matriz africana"),

	OUTRAS("Outras");

	private final String rotulo;

	CrencaReligiosa(String rotulo) {
		this.rotulo = rotulo;
	}

	/** Nome exibido nas telas e nas exportações. */
	public String rotulo() {
		return rotulo;
	}

}
