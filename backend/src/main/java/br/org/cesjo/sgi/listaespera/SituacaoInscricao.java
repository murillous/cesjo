package br.org.cesjo.sgi.listaespera;

/**
 * Situação de uma inscrição na lista de espera (RF002, ADR 0091). Só as inscrições {@link #AGUARDANDO}
 * ocupam posição na fila.
 */
public enum SituacaoInscricao {

	AGUARDANDO("Aguardando"),

	/** Entrou no serviço (RF007). Só o cadastro da inserção muda para esta situação. */
	INSERIDO("Inserido no serviço"),

	// PENDENTE-CESJO(P4): a saída da lista é uma situação só, com o motivo em MotivoSaida (ADR 0091).
	/** Saiu da lista sem ser inserida, com um {@link MotivoSaida}. */
	SAIU("Saiu da lista");

	private final String rotulo;

	SituacaoInscricao(String rotulo) {
		this.rotulo = rotulo;
	}

	public String rotulo() {
		return rotulo;
	}

}
