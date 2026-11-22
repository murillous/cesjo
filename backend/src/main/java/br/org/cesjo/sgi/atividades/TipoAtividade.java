package br.org.cesjo.sgi.atividades;

/**
 * Tipos de atividade do RF010 (ADR 0150 e ADR 0154). O nome de cada constante é o valor guardado na
 * coluna {@code atividade.tipo}; mudar um nome pede migração.
 */
public enum TipoAtividade {

	/** Atividade com as famílias dos usuários atendidos. */
	COM_FAMILIAS(false, 1),

	/** Atividade com os usuários atendidos. */
	COM_USUARIOS(false, 1),

	/** Atividade que envolve os usuários atendidos e as suas famílias. */
	COM_USUARIOS_E_FAMILIAS(false, 1),

	// PENDENTE-CESJO(P25): o atendimento multiprofissional exige pelo menos dois profissionais (ADR 0154).
	/** Atendimento de um usuário por mais de um profissional. */
	ATENDIMENTO_MULTIPROFISSIONAL(true, 2),

	/** Atendimento técnico individualizado de um usuário. */
	ATENDIMENTO_TECNICO_INDIVIDUALIZADO(true, 1),

	/** Estudo de caso de um usuário pela equipe. */
	ESTUDO_DE_CASO(true, 1);

	private final boolean individual;

	private final int minimoProfissionais;

	TipoAtividade(boolean individual, int minimoProfissionais) {
		this.individual = individual;
		this.minimoProfissionais = minimoProfissionais;
	}

	/**
	 * Atendimento ou estudo de caso: ligado a um só usuário atendido, com leitura da descrição auditada
	 * como dado sensível.
	 */
	public boolean individual() {
		return individual;
	}

	/** Quantos profissionais, no mínimo, a atividade deste tipo precisa ter. */
	public int minimoProfissionais() {
		return minimoProfissionais;
	}

}
