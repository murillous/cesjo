package br.org.cesjo.sgi.listaespera;

/** Motivo de saída da lista de espera sem inserção no serviço (RF002, ADR 0091). */
public enum MotivoSaida {

	// PENDENTE-CESJO(P4): lista provisória, a partir dos exemplos da pergunta 4; "Outro" exige
	// observação (ADR 0091).

	DESISTENCIA("Desistência"),

	NAO_LOCALIZADO("Não localizado"),

	FORA_DO_PERFIL("Fora do perfil do serviço"),

	OUTRO("Outro motivo");

	private final String rotulo;

	MotivoSaida(String rotulo) {
		this.rotulo = rotulo;
	}

	public String rotulo() {
		return rotulo;
	}

}
