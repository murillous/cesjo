package br.org.cesjo.sgi.atendidos;

/** Situação da inserção no serviço. Só as inserções {@link #ATIVA} ocupam vaga (RF003). */
public enum SituacaoInsercao {

	ATIVA("Ativo"),

	/** Saiu do serviço por desligamento (RF008, issue #42). */
	DESLIGADA("Desligado");

	private final String rotulo;

	SituacaoInsercao(String rotulo) {
		this.rotulo = rotulo;
	}

	/** Nome exibido nas telas, falando da pessoa: "Ativo", "Desligado". */
	public String rotulo() {
		return rotulo;
	}

}
