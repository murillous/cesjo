package br.org.cesjo.sgi.atendidos;

/**
 * As 12 categorias de violação de direitos do RF009. Abuso sexual e violência física ou psicológica
 * pedem o âmbito, intra ou extrafamiliar ({@link #exigeAmbito()}).
 */
public enum TipoViolacao {

	EXPLORACAO_IMAGEM("Exploração de imagem", false),

	ISOLAMENTO_SOCIAL("Isolamento social", false),

	CONFINAMENTO("Confinamento", false),

	DISCRIMINACAO_FAMILIAR("Atitudes discriminatórias e preconceituosas no seio familiar", false),

	FALTA_CUIDADOS_CUIDADOR("Falta de cuidados adequados por parte do cuidador familiar", false),

	ESTRESSE_CUIDADOR("Alto grau de estresse do cuidador familiar", false),

	DESVALORIZACAO_CAPACIDADE("Desvalorização da potencialidade ou capacidade da pessoa", false),

	VIOLENCIA_PATRIMONIAL("Violência patrimonial", false),

	NEGLIGENCIA_ABANDONO("Negligência ou abandono", false),

	ABUSO_SEXUAL("Abuso sexual", true),

	VIOLENCIA_FISICA_PSICOLOGICA("Violência física ou psicológica", true),

	OUTRAS("Outras violações", false);

	private final String rotulo;

	private final boolean exigeAmbito;

	TipoViolacao(String rotulo, boolean exigeAmbito) {
		this.rotulo = rotulo;
		this.exigeAmbito = exigeAmbito;
	}

	public String rotulo() {
		return rotulo;
	}

	/** O RF009 divide a categoria em intra e extrafamiliar. */
	public boolean exigeAmbito() {
		return exigeAmbito;
	}

}
