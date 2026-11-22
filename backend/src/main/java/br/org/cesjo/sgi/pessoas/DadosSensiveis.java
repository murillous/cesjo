package br.org.cesjo.sgi.pessoas;

/**
 * Crença religiosa e dados de saúde do usuário atendido (RF007, ADR 0140). Só quem tem a permissão de
 * dados sensíveis lê e edita (ADR 0142). Os textos de saúde são cifrados; a crença é categórica e fica
 * em claro, como manda o ADR 0004 (ADR 0141). Todos opcionais.
 *
 * @param medicamentosUsoContinuo medicamentos de uso contínuo, com dose e horário
 * @param observacoesSaude alergias, restrições e outras condições de saúde
 */
// PENDENTE-CESJO(P6): dados de saúde provisórios do cadastro completo (ADR 0140).
public record DadosSensiveis(CrencaReligiosa crencaReligiosa, String medicamentosUsoContinuo,
		String observacoesSaude) {

	public static final DadosSensiveis VAZIO = new DadosSensiveis(null, null, null);

	public DadosSensiveis {
		medicamentosUsoContinuo = CadastroComplementar.texto(medicamentosUsoContinuo);
		observacoesSaude = CadastroComplementar.texto(observacoesSaude);
	}

	public boolean vazio() {
		return equals(VAZIO);
	}

}
