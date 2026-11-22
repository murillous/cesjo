package br.org.cesjo.sgi.pessoas;

/**
 * Gênero da pessoa, do cadastro completo do usuário atendido (RF007, ADR 0143). Sem valor quer dizer
 * "não informado". Fica em claro para os dashboards contarem por gênero (ADR 0141).
 */
public enum Genero {

	// PENDENTE-CESJO(P21): opções de gênero provisórias (ADR 0143).

	FEMININO("Feminino"),

	MASCULINO("Masculino"),

	OUTRO("Outro");

	private final String rotulo;

	Genero(String rotulo) {
		this.rotulo = rotulo;
	}

	/** Nome exibido nas telas e nas exportações. */
	public String rotulo() {
		return rotulo;
	}

}
